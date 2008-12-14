// Copyright 2008 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.opengse.blockingcore;


import javax.servlet.ServletInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.BufferedReader;
import java.util.Enumeration;
import java.util.List;
import java.util.Iterator;
import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * @author jennings
 *         Date: Jul 27, 2008
 */
class ServletInputStreamImpl extends ServletInputStream {
  private static final int CR = '\r';
  private static final int LF = '\n';
  private InputStream realStream;
  private static final int MAX_HEADERS_SIZE = 100000;
  private HttpRequestType requestType;
  private HttpHeaders headers;

  ServletInputStreamImpl(InputStream realStream) throws IOException {
    this.realStream = new BufferedInputStream(realStream);
    headers = new HttpHeaders();
    readHeaders();
  }

  private void readHeaders() throws IOException {
    int thebyte;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    int pattern = -1;

    while (baos.size() < MAX_HEADERS_SIZE) {
      thebyte = realStream.read();
      if (thebyte == -1) {
        break;
      }
      baos.write(thebyte);
      if (thebyte == CR) {
        if (pattern == -1 || pattern == 1) {
          ++pattern;
        } else {
          pattern = -1;
        }
      } else if (thebyte == LF) {
        if (pattern == 0 || pattern == 2) {
          ++pattern;
          if (pattern == 3) {
            // this indicates that we have encountered 2 pairs of cr/lf bytes
            break;
          }
        }
      } else {
        pattern = -1;
      }
    }
    String allHeaders = baos.toString();
    BufferedReader reader = new BufferedReader(new StringReader(allHeaders));
    requestType = new HttpRequestType(reader.readLine());
    headers.readHeaders(reader);
  }

  public int read() throws IOException {
    return realStream.read();
  }

  @Override
  public int readLine(byte[] b, int off, int len) throws IOException {
    return super.readLine(b, off, len);
  }

  @Override
  public int read(byte b[], int off, int len) throws IOException {
    return realStream.read(b, off, len);    
  }

  Enumeration<String> getHeaders(String name) {
    return new StringEnumerator(headers.getHeaderValues(name));
  }

  String getHeader(String name) {
    List<String> values = headers.getHeaderValues(name);
    if (values.isEmpty()) {
      return null;
    }
    return values.iterator().next();
  }

  Enumeration<String> getHeaderNames() {
    return new StringEnumerator(headers.getHeaderNames());
  }

  HttpRequestType getRequestType() {
    return requestType;
  }

  private static class StringEnumerator implements Enumeration<String> {
    private Iterator<String> iterator_;

    public StringEnumerator(Collection<String> headers) {
      this.iterator_ = headers.iterator();
    }

    public boolean hasMoreElements() {
      return iterator_.hasNext();
    }

    public String nextElement()
      throws NoSuchElementException {
      return iterator_.next();
    }
  }
  
}
