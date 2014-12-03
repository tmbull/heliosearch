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

package org.apache.solr.common.params;

import org.apache.solr.common.util.StrUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.io.IOException;

/**
 *
 */
public class MultiMapSolrParams extends SolrParams {
  protected final Map<String,String[]> map;

  public static void addParam(String name, String val, Map<String,String[]> map) {
      String[] arr = map.get(name);
      if (arr == null) {
        arr = new String[]{val};
      } else {
        String[] newarr = new String[arr.length+1];
        System.arraycopy(arr, 0, newarr, 0, arr.length);
        newarr[arr.length] = val;
        arr = newarr;
      }
      map.put(name, arr);
  }

  public static void addParam(String name, String[] vals, Map<String,String[]> map) {
    String[] arr = map.put(name, vals);
    if (arr == null) {
      return;
    }

    String[] newarr = new String[arr.length+vals.length];
    System.arraycopy(arr, 0, newarr, 0, arr.length);
    System.arraycopy(vals, 0, newarr, arr.length, vals.length);
    arr = newarr;

    map.put(name, arr);
  }

  public MultiMapSolrParams(Map<String,String[]> map) {
    this.map = map;
  }

  @Override
  public String get(String name) {
    String[] arr = map.get(name);
    return arr==null ? null : arr[0];
  }

  @Override
  public String[] getParams(String name) {
    return map.get(name);
  }

  @Override
  public Iterator<String> getParameterNamesIterator() {
    return map.keySet().iterator();
  }

  public Map<String,String[]> getMap() { return map; }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(128);
    try {
      appendHttpParams(sb, -1);
      return sb.toString();
    }
    catch (IOException e) {throw new RuntimeException();}  // can't happen
  }

  @Override
  public void appendHttpParams(Appendable out, int maxParamSize) throws IOException {
    boolean first=true;

    for (Map.Entry<String,String[]> entry : map.entrySet()) {
      String key = entry.getKey();
      String[] valarr = entry.getValue();

      for (String val : valarr) {
        if (!first) out.append('&');
        first=false;
        appendHttpParam(out, key, val, maxParamSize);
      }
    }
  }

  public static Map<String,String[]> asMultiMap(SolrParams params) {
    return asMultiMap(params, false);
  }

  public static Map<String,String[]> asMultiMap(SolrParams params, boolean newCopy) {
    if (params instanceof MultiMapSolrParams) {
      Map<String,String[]> map = ((MultiMapSolrParams)params).getMap();
      if (newCopy) {
        return new HashMap<>(map);
      }
      return map;
    } else if (params instanceof ModifiableSolrParams) {
      Map<String,String[]> map = ((ModifiableSolrParams)params).getMap();
      if (newCopy) {
        return new HashMap<>(map);
      }
      return map;
    } else {
      Map<String,String[]> map = new HashMap<>();
      Iterator<String> iterator = params.getParameterNamesIterator();
      while (iterator.hasNext()) {
        String name = iterator.next();
        map.put(name, params.getParams(name));
      }
      return map;
    }
  }

}
