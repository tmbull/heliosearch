/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.solr.search;

import org.apache.solr.search.facet.AggValueSource;
import org.apache.solr.search.function.FunctionQuery;
import org.apache.solr.search.function.ValueSource;
import org.apache.solr.search.function.valuesource.*;
import org.apache.lucene.search.Query;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.SchemaField;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FunctionQParser extends QParser {

  public static final int FLAG_CONSUME_DELIMITER = 0x01;  // consume delimiter after parsing arg
  public static final int FLAG_IS_AGG = 0x02;
  public static final int FLAG_DEFAULT = FLAG_CONSUME_DELIMITER;

  /** @lucene.internal */
  public QueryParsing.StrParser sp;
  boolean parseMultipleSources = true;
  boolean parseToEnd = true;

  public FunctionQParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
    super(qstr, localParams, params, req);
    setString(qstr);
  }

  @Override
  public void setString(String s) {
    super.setString(s);
    if (s != null) {
      sp = new QueryParsing.StrParser( s );
    }
  }

  public void setParseMultipleSources(boolean parseMultipleSources) {
    this.parseMultipleSources = parseMultipleSources;  
  }

  /** parse multiple comma separated value sources */
  public boolean getParseMultipleSources() {
    return parseMultipleSources;
  }

  public void setParseToEnd(boolean parseToEnd) {
    this.parseToEnd = parseToEnd;
  }

  /** throw exception if there is extra stuff at the end of the parsed valuesource(s). */
  public boolean getParseToEnd() {
    return parseMultipleSources;
  }

  @Override
  public Query parse() throws SyntaxError {
    ValueSource vs = null;
    List<ValueSource> lst = null;

    for(;;) {
      ValueSource valsource = parseValueSource(FLAG_DEFAULT & ~FLAG_CONSUME_DELIMITER);
      sp.eatws();
      if (!parseMultipleSources) {
        vs = valsource; 
        break;
      } else {
        if (lst != null) {
          lst.add(valsource);
        } else {
          vs = valsource;
        }
      }

      // check if there is a "," separator
      if (sp.peek() != ',') break;

      consumeArgumentDelimiter();

      if (lst == null) {
        lst = new ArrayList<>(2);
        lst.add(valsource);
      }
    }

    if (parseToEnd && sp.pos < sp.end) {
      throw new SyntaxError("Unexpected text after function: " + sp.val.substring(sp.pos, sp.end));
    }

    if (lst != null) {
      vs = new VectorValueSource(lst);
    }

    return new FunctionQuery(vs);
  }


  /**
   * Are there more arguments in the argument list being parsed?
   * 
   * @return whether more args exist
   */
  public boolean hasMoreArguments() throws SyntaxError {
    int ch = sp.peek();
    /* determine whether the function is ending with a paren or end of str */
    return (! (ch == 0 || ch == ')') );
  }
  
  /*
   * TODO: Doc
   */
  public String parseId() throws SyntaxError {
    String value = parseArg();
    if (argWasQuoted) throw new SyntaxError("Expected identifier instead of quoted string:" + value);
    return value;
  }
  
  /**
   * Parse a float.
   * 
   * @return Float
   */
  public Float parseFloat() throws SyntaxError {
    String str = parseArg();
    if (argWasQuoted()) throw new SyntaxError("Expected float instead of quoted string:" + str);
    float value = Float.parseFloat(str);
    return value;
  }

  /**
   * Parse a Double
   * @return double
   */
  public double parseDouble() throws SyntaxError {
    String str = parseArg();
    if (argWasQuoted()) throw new SyntaxError("Expected double instead of quoted string:" + str);
    double value = Double.parseDouble(str);
    return value;
  }

  /**
   * Parse an integer
   * @return An int
   */
  public int parseInt() throws SyntaxError {
    String str = parseArg();
    if (argWasQuoted()) throw new SyntaxError("Expected double instead of quoted string:" + str);
    int value = Integer.parseInt(str);
    return value;
  }


  private boolean argWasQuoted;
  public boolean argWasQuoted() {
    return argWasQuoted;
  }

  public String parseArg() throws SyntaxError {
    argWasQuoted = false;

    sp.eatws();
    char ch = sp.peek();
    String val = null;
    switch (ch) {
      case ')': return null;
      case '$':
        sp.pos++;
        String param = sp.getId();
        val = getParam(param);
        break;
      case '\'':
      case '"':
        val = sp.getQuotedString();
        argWasQuoted = true;
        break;
      default:
        // read unquoted literal ended by whitespace ',' or ')'
        // there is no escaping.
        int valStart = sp.pos;
        for (;;) {
          if (sp.pos >= sp.end) {
            throw new SyntaxError("Missing end to unquoted value starting at " + valStart + " str='" + sp.val +"'");
          }
          char c = sp.val.charAt(sp.pos);
          if (c==')' || c==',' || Character.isWhitespace(c)) {
            val = sp.val.substring(valStart, sp.pos);
            break;
          }
          sp.pos++;
        }
    }

    sp.eatws();
    consumeArgumentDelimiter();
    return val;
  }

  
  /**
   * Parse a list of ValueSource.  Must be the final set of arguments
   * to a ValueSource.
   * 
   * @return List&lt;ValueSource&gt;
   */
  public List<ValueSource> parseValueSourceList() throws SyntaxError {
    List<ValueSource> sources = new ArrayList<>(3);
    while (hasMoreArguments()) {
      sources.add(parseValueSource(FLAG_DEFAULT));
    }
    return sources;
  }

  /**
   * Parse an individual ValueSource.
   */
  public ValueSource parseValueSource() throws SyntaxError {
    /* consume the delimiter afterward for an external call to parseValueSource */
    return parseValueSource(FLAG_DEFAULT);
  }
  
  /*
   * TODO: Doc
   */
  public Query parseNestedQuery() throws SyntaxError {
    Query nestedQuery;
    
    if (sp.opt("$")) {
      String param = sp.getId();
      String qstr = getParam(param);
      qstr = qstr==null ? "" : qstr;
      nestedQuery = subQuery(qstr, null).getQuery();
    }
    else {
      int start = sp.pos;
      String v = sp.val;
  
      String qs = v;
      HashMap nestedLocalParams = new HashMap<String,String>();
      int end = QueryParsing.parseLocalParams(qs, start, nestedLocalParams, getParams());
  
      QParser sub;
  
      if (end>start) {
        if (nestedLocalParams.get(QueryParsing.V) != null) {
          // value specified directly in local params... so the end of the
          // query should be the end of the local params.
          sub = subQuery(qs.substring(start, end), null);
        } else {
          // value here is *after* the local params... ask the parser.
          sub = subQuery(qs, null);
          // int subEnd = sub.findEnd(')');
          // TODO.. implement functions to find the end of a nested query
          throw new SyntaxError("Nested local params must have value in v parameter.  got '" + qs + "'");
        }
      } else {
        throw new SyntaxError("Nested function query must use $param or {!v=value} forms. got '" + qs + "'");
      }
  
      sp.pos += end-start;  // advance past nested query
      nestedQuery = sub.getQuery();
    }
    consumeArgumentDelimiter();
    
    return nestedQuery;
  }

  /**
   * Parse an individual value source.
   * 
   * @flags controls parsing of the value source
   */
  protected ValueSource parseValueSource(int flags) throws SyntaxError {
    ValueSource valueSource;
    
    int ch = sp.peek();
    if (ch>='0' && ch<='9'  || ch=='.' || ch=='+' || ch=='-') {
      Number num = sp.getNumber();
      if (num instanceof Long) {
        valueSource = new LongConstValueSource(num.longValue());
      } else if (num instanceof Double) {
        valueSource = new DoubleConstValueSource(num.doubleValue());
      } else {
        // shouldn't happen
        valueSource = new ConstValueSource(num.floatValue());
      }
    } else if (ch == '"' || ch == '\''){
      valueSource = new LiteralValueSource(sp.getQuotedString());
    } else if (ch == '$') {
      sp.pos++;
      String param = sp.getId();
      String val = getParam(param);
      if (val == null) {
        throw new SyntaxError("Missing param " + param + " while parsing function '" + sp.val + "'");
      }

      QParser subParser = subQuery(val, "func");
      if (subParser instanceof FunctionQParser) {
        ((FunctionQParser)subParser).setParseMultipleSources(true);
      }
      Query subQuery = subParser.getQuery();
      if (subQuery instanceof FunctionQuery) {
        valueSource = ((FunctionQuery) subQuery).getValueSource();
      } else {
        valueSource = new QueryValueSource(subQuery, 0.0f);
      }

      /***
       // dereference *simple* argument (i.e., can't currently be a function)
       // In the future we could support full function dereferencing via a stack of ValueSource (or StringParser) objects
      ch = val.length()==0 ? '\0' : val.charAt(0);

      if (ch>='0' && ch<='9'  || ch=='.' || ch=='+' || ch=='-') {
        QueryParsing.StrParser sp = new QueryParsing.StrParser(val);
        Number num = sp.getNumber();
        if (num instanceof Long) {
          valueSource = new LongConstValueSource(num.longValue());
        } else if (num instanceof Double) {
          valueSource = new DoubleConstValueSource(num.doubleValue());
        } else {
          // shouldn't happen
          valueSource = new ConstValueSource(num.floatValue());
        }
      } else if (ch == '"' || ch == '\'') {
        QueryParsing.StrParser sp = new QueryParsing.StrParser(val);
        val = sp.getQuotedString();
        valueSource = new LiteralValueSource(val);
      } else {
        if (val.length()==0) {
          valueSource = new LiteralValueSource(val);
        } else {
          String id = val;
          SchemaField f = req.getSchema().getField(id);
          valueSource = f.getType().getValueSource(f, this);
        }
      }
       ***/

    } else {

      String id = sp.getId();
      if (sp.opt("(")) {
        // a function... look it up.
        ValueSourceParser argParser = req.getCore().getValueSourceParser(id);
        if (argParser==null) {
          throw new SyntaxError("Unknown function " + id + " in FunctionQuery(" + sp + ")");
        }
        valueSource = argParser.parse(this);
        sp.expect(")");
      }
      else {
        if ("true".equals(id)) {
          valueSource = new BoolConstValueSource(true);
        } else if ("false".equals(id)) {
          valueSource = new BoolConstValueSource(false);
        } else {
          SchemaField f = req.getSchema().getField(id);
          valueSource = f.getType().getValueSource(f, this);
        }
      }

    }
    
    if ((flags & FLAG_CONSUME_DELIMITER) != 0) {
      consumeArgumentDelimiter();
    }
    
    return valueSource;
  }

  public AggValueSource parseAgg(int flags) throws SyntaxError {
    String id = sp.getId();
    AggValueSource vs = null;
    boolean hasParen = false;

    if ("agg".equals(id)) {
      hasParen = sp.opt("(");
      vs = parseAgg(flags | FLAG_IS_AGG);
    } else {
      // parse as an aggregation...
      if (!id.startsWith("agg_")) {
        id = "agg_" + id;
      }

      hasParen = sp.opt("(");

      ValueSourceParser argParser = req.getCore().getValueSourceParser(id);
      argParser = req.getCore().getValueSourceParser(id);
      if (argParser == null) {
        throw new SyntaxError("Unknown aggregation " + id + " in (" + sp + ")");
      }

      ValueSource vv = argParser.parse(this);
      if (!(vv instanceof AggValueSource)) {
        if (argParser == null) {
          throw new SyntaxError("Expected aggregation from " + id + " but got (" + vv + ") in (" + sp + ")");
        }
      }
      vs = (AggValueSource) vv;
    }

    if (hasParen) {
      sp.expect(")");
    }

    if ((flags & FLAG_CONSUME_DELIMITER) != 0) {
      consumeArgumentDelimiter();
    }

    return vs;
  }


  /**
   * Consume an argument delimiter (a comma) from the token stream.
   * Only consumes if more arguments should exist (no ending parens or end of string).
   * 
   * @return whether a delimiter was consumed
   */
  protected boolean consumeArgumentDelimiter() throws SyntaxError {
    /* if a list of args is ending, don't expect the comma */
    if (hasMoreArguments()) {
      sp.expect(",");
      return true;
    }
   
    return false;
  }
    

}
