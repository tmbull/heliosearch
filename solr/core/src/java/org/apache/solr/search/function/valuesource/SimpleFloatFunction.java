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

package org.apache.solr.search.function.valuesource;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.solr.search.QueryContext;
import org.apache.solr.search.function.FuncValues;
import org.apache.solr.search.function.ValueSource;
import org.apache.solr.search.function.funcvalues.FloatFuncValues;

import java.io.IOException;
import java.util.Map;

/**
 * A simple float function with a single argument
 */
public abstract class SimpleFloatFunction extends SingleFunction {
  public SimpleFloatFunction(ValueSource source) {
    super(source);
  }

  protected abstract float func(int doc, FuncValues vals);

  @Override
  public FuncValues getValues(QueryContext context, AtomicReaderContext readerContext) throws IOException {
    final FuncValues vals = source.getValues(context, readerContext);
    return new FloatFuncValues(this) {
      @Override
      public float floatVal(int doc) {
        return func(doc, vals);
      }

      @Override
      public String toString(int doc) {
        return name() + '(' + vals.toString(doc) + ')';
      }
    };
  }
}