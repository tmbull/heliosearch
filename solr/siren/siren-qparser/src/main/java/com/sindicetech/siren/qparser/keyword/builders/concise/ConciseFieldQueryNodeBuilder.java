/**
 * Copyright (c) 2014, Sindice Limited. All Rights Reserved.
 *
 * This file is part of the SIREn project.
 *
 * SIREn is a free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * SIREn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.sindicetech.siren.qparser.keyword.builders.concise;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.config.QueryConfigHandler;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;

import com.sindicetech.siren.qparser.keyword.builders.FieldQueryNodeBuilder;
import com.sindicetech.siren.qparser.keyword.config.ConciseKeywordQueryConfigHandler;
import com.sindicetech.siren.search.node.NodeQuery;

/**
 * An extension of the {@link com.sindicetech.siren.qparser.keyword.builders.FieldQueryNodeBuilder} that will prepend
 * the {@link com.sindicetech.siren.qparser.keyword.config.ConciseKeywordQueryConfigHandler.ConciseKeywordConfigurationKeys#ATTRIBUTE} to the encoded value
 * of the {@link org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode} before delegating the construction
 * of the {@link NodeQuery} to the original {@link FieldQueryNodeBuilder}.
 */
public class ConciseFieldQueryNodeBuilder extends FieldQueryNodeBuilder {

  private final QueryConfigHandler conf;

  private final StringBuilder builder = new StringBuilder();

  public ConciseFieldQueryNodeBuilder(final QueryConfigHandler queryConf) {
    this.conf = queryConf;
  }

  @Override
  public NodeQuery build(final QueryNode queryNode) throws QueryNodeException {
    if (conf.has(ConciseKeywordQueryConfigHandler.ConciseKeywordConfigurationKeys.ATTRIBUTE)) {
      final String attribute = conf.get(ConciseKeywordQueryConfigHandler.ConciseKeywordConfigurationKeys.ATTRIBUTE);

      final FieldQueryNode fieldNode = (FieldQueryNode) queryNode;

      // Prepend the attribute to the query term
      ConciseNodeBuilderUtil.prepend(builder, attribute, fieldNode);
    }

    // Delegate the build to parent class
    return super.build(queryNode);
  }

}
