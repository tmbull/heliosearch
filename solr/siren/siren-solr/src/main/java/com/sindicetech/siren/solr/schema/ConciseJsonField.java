/**
 * Copyright (c) 2014, Sindice Limited. All Rights Reserved.
 *
 * This file is part of the SIREn project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sindicetech.siren.solr.schema;

import com.sindicetech.siren.solr.analysis.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.solr.analysis.TokenizerChain;
import org.apache.solr.common.SolrException;
import org.apache.solr.schema.IndexSchema;

import java.util.HashMap;
import java.util.Map;

/**
 * The ConciseJsonField is an extension of the {@link ExtendedJsonField} for
 * configurable JSON data analysis using a concise tree model.
 * <p>
 * This field type relies on the {@link com.sindicetech.siren.solr.analysis.PathEncodingFilterFactory} to encode
 * {@link com.sindicetech.siren.analysis.attributes.PathAttribute}s.
 * {@link com.sindicetech.siren.analysis.attributes.PathAttribute}s are generated by tokenizers for the concise model, for
 * example, the {@link com.sindicetech.siren.analysis.ConciseJsonTokenizer}.
 * <p>
 * This field type can be configured to keep the original token with the parameter
 * {@link com.sindicetech.siren.solr.analysis.PathEncodingFilterFactory#ATTRIBUTEWILDCARD_KEY}.
 *
 * @see com.sindicetech.siren.solr.analysis.PathEncodingFilterFactory
 * @see ExtendedJsonField
 */
public class ConciseJsonField extends ExtendedJsonField {

  private boolean hasAttributeWildcard;
  private boolean attributeWildcard;

  @Override
  protected void init(final IndexSchema schema, final Map<String,String> args) {
    super.init(schema, args);
    // initialise PathEncodingFilter's option attributeWildcard
    if (args.containsKey(PathEncodingFilterFactory.ATTRIBUTEWILDCARD_KEY)) {
      this.hasAttributeWildcard = true;
      this.attributeWildcard = Boolean.parseBoolean(args.remove(PathEncodingFilterFactory.ATTRIBUTEWILDCARD_KEY));
    }
  }

  @Override
  protected TokenizerFactory getTokenizerFactory(final Map<String,String> args) {
    return new ConciseJsonTokenizerFactory(args);
  }

  /**
   * Load the config when resource loader initialized.
   *
   * @param resourceLoader The resource loader.
   */
  @Override
  public void inform(final ResourceLoader resourceLoader) {
    super.inform(resourceLoader);

    // if there was a attributeWildcard parameter defined, updates the configuration of the PathEncodingFilterFactory
    if (this.hasAttributeWildcard) {
      final TokenizerChain chain = (TokenizerChain) this.getIndexAnalyzer();
      for (TokenFilterFactory tokenFilterFactory : chain.getTokenFilterFactories()) {
        if (tokenFilterFactory instanceof PathEncodingFilterFactory) {
          ((PathEncodingFilterFactory) tokenFilterFactory).setAttributeWildcard(this.attributeWildcard);
        }
      }
    }
  }

  /**
   * Append the mandatory SIREn filters for the concise model, i.e.,
   * {@link com.sindicetech.siren.solr.analysis.DatatypeAnalyzerFilterFactory},
   * {@link com.sindicetech.siren.solr.analysis.PathEncodingFilterFactory},
   * {@link com.sindicetech.siren.solr.analysis.PositionAttributeFilterFactory} and
   * {@link com.sindicetech.siren.solr.analysis.SirenPayloadFilterFactory}, to the tokenizer chain.
   *
   * @see ExtendedJsonField#appendSirenFilters(org.apache.lucene.analysis.Analyzer, java.util.Map)
   */
  @Override
  protected Analyzer appendSirenFilters(final Analyzer analyzer,
                                        final Map<String, Datatype> datatypes) {
    if (!(analyzer instanceof TokenizerChain)) {
      throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
        "Invalid index analyzer '" + analyzer.getClass() + "' received");
    }

    final TokenizerChain chain = (TokenizerChain) analyzer;
    // copy the existing list of token filters
    final TokenFilterFactory[] old = chain.getTokenFilterFactories();
    final TokenFilterFactory[] filterFactories = new TokenFilterFactory[old.length + 4];
    System.arraycopy(old, 0, filterFactories, 0, old.length);
    // append the datatype analyzer filter factory
    final DatatypeAnalyzerFilterFactory datatypeFactory = new DatatypeAnalyzerFilterFactory(new HashMap<String, String>());
    datatypeFactory.register(datatypes);
    filterFactories[old.length] = datatypeFactory;
    // append the path encoding filter factory
    filterFactories[old.length + 1] = new PathEncodingFilterFactory(new HashMap<String,String>());
    // append the position attribute filter factory
    filterFactories[old.length + 2] = new PositionAttributeFilterFactory(new HashMap<String,String>());
    // append the siren payload filter factory
    filterFactories[old.length + 3] = new SirenPayloadFilterFactory(new HashMap<String,String>());
    // create a new tokenizer chain with the updated list of filter factories
    return new TokenizerChain(chain.getCharFilterFactories(), chain.getTokenizerFactory(), filterFactories);
  }

}

