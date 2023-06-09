/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mapr.drill.maprdb.tests.index;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class LargeTableGenBase {

  private boolean dict_ready = false;

  protected List<String> firstnames;
  protected List<String> lastnames;
  protected List<String[]> cities;
  protected int[] randomized;

  protected synchronized void  initDictionary() {
    initDictionaryWithRand();
  }

  protected void initDictionaryWithRand() {
    {
      firstnames = new ArrayList<>();
      lastnames = new ArrayList<>();
      cities = new ArrayList<>();
      List<String> states = new ArrayList<>();

      int fnNum = 2000;     // 2k
      int lnNum = 200000;   // 200k
      int cityNum = 10000;  // 10k
      int stateNum = 50;
      Random rand = new Random(2017);
      int i;
      try {
        Set<String> strSet = new LinkedHashSet<>();
        while (strSet.size() < stateNum) {
          strSet.add(RandomStringUtils.random(2, 0, 0, true, false, null, rand));
        }
        states.addAll(strSet);

        strSet = new LinkedHashSet<>();
        while (strSet.size() < cityNum) {
          int len = 3 + strSet.size() % 6;
          strSet.add(RandomStringUtils.random(len, 0, 0, true, false, null, rand));
        }

        Iterator<String> it = strSet.iterator();
        for (i = 0; i < cityNum; ++i) {
          cities.add(new String[]{"10000", states.get(i%stateNum),  it.next()});
        }

        strSet = new LinkedHashSet<>();
        while (strSet.size() < fnNum) {
          int len = 3 + strSet.size() % 6;
          strSet.add(RandomStringUtils.random(len, 0, 0, true, false, null, rand));
        }
        firstnames.addAll(strSet);

        strSet = new LinkedHashSet<>();
        while (strSet.size() < lnNum) {
          int len = 3 + strSet.size() % 6;
          strSet.add(RandomStringUtils.random(len, 0, 0, true, false, null, rand));
        }
        lastnames.addAll(strSet);
      } catch(Exception e) {
        System.out.println("init data got exception");
        e.printStackTrace();
      }
      dict_ready = true;
    }
  }

  protected  String getFirstName(int i) {
    return firstnames.get((randomized[ i%randomized.length ] + i )% firstnames.size());
  }

  protected String getLastName(int i) {
    return lastnames.get((randomized[ (2*i + randomized[i%randomized.length])% randomized.length]) % lastnames.size());
  }

  protected String[] getAddress(int i) {
    return cities.get((randomized[(i+ randomized[i%randomized.length])%randomized.length]) % cities.size());
  }

  protected String getSSN(int i){
    return String.format("%d", 1000*1000*100 + randomized[ i % randomized.length]);
  }

  protected String getPhone(int i) {
    // 80% phones are unique,
    return String.format("%d", 6500*1000*1000L + randomized[ (randomized.length - i) %((int) (randomized.length * 0.8)) ]);
  }

  protected String getEmail(int i){
    return getFirstName(i) + getLastName(i) + "@" + "gmail.com";
  }

  protected String getAge(int i) {
    return String.format("%d",randomized[i%randomized.length] % 60 + 10);
  }

  protected String getIncome(int i) {//unit should be $10k
    return String.format("%d",randomized[i%randomized.length] % 47 + 1);
  }

  // date yyyy-mm-dd
  protected String getBirthdate(int i) {
    int thisseed = randomized[i%randomized.length];
    return String.format("%d-%02d-%02d",
        2016 - (thisseed % 60 + 10), thisseed % 12 + 1, (thisseed * 31) % 28 + 1 );
  }

  // timestamp, yyyy-mm-dd HH:mm:ss
  protected String getFirstLogin(int i) {
    int thisseed = randomized[i%randomized.length];
    int nextseed = randomized[(i+1)%randomized.length];
    return String.format("%d-%02d-%02d %02d:%02d:%02d.0",
        2016 - (thisseed % 7), (thisseed * 31) % 12 + 1, thisseed % 28 + 1, nextseed % 24, nextseed % 60, (nextseed * 47) % 60 );
  }


  protected String getField(String field, int i) {
    if(field.equals("ssn")) {
      return getSSN(i);
    }
    else if (field.equals("phone")) {
      return getPhone(i);
    }
    else if(field.equals("email")) {
      return getEmail(i);
    }
    else if(field.equals("city")) {
      return getAddress(i)[1];
    }
    else if(field.equals("state")) {
      return getAddress(i)[0];
    }
    else if(field.equals("fname")) {
      return getFirstName(i);
    }
    else if(field.equals("lname")) {
      return getLastName(i);
    }
    return "";
  }


  protected void initRandVector(int recordNumber) {
    int i;
    Random rand = new Random(2016);
    randomized = new int[recordNumber];
    for(i = 0; i<recordNumber; ++i) {
      randomized[i] = i;
    }
    for (i=0; i<recordNumber; ++i) {
      int idx1 =  rand.nextInt(recordNumber);
      int idx2 = rand.nextInt(recordNumber);
      int temp = randomized[idx1];
      randomized[idx1] = randomized[idx2];
      randomized[idx2] = temp;
    }
  }

}
