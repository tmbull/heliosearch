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

import org.apache.solr.search.QueryContext;
import org.apache.solr.search.function.ValueSource;

import java.io.IOException;

/**
 * A function with a single argument
 */
public abstract class SingleFunction extends ValueSource {
  protected final ValueSource source;

  public SingleFunction(ValueSource source) {
    this.source = source;
  }

  protected abstract String name();

  @Override
  public String description() {
    return name() + '(' + source.description() + ')';
  }

  @Override
  public int hashCode() {
    return source.hashCode() + name().hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this.getClass() != o.getClass()) return false;
    SingleFunction other = (SingleFunction) o;
    return this.name().equals(other.name())
        && this.source.equals(other.source);
  }

  @Override
  public void createWeight(QueryContext context) throws IOException {
    source.createWeight(context);
  }
}