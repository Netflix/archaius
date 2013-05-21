/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.config.sources;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Maps.filterValues;
import static com.google.common.collect.Maps.transformEntries;
import static org.jclouds.util.Strings2.toStringAndClose;

import java.io.IOException;
import java.util.Map;

import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobMap;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps.EntryTransformer;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.config.PollResult;
import com.netflix.config.PolledConfigurationSource;

/**
 * {@link PolledConfigurationSource} backed by any supported jclouds
 * {@link BlobStore}, such as {@code aws-s3}, {@code swift}, etc.
 *
 * <h3>Storage</h3>
 *
 * Config entries are stored in a container configured by the
 * property {@code com.netflix.config.blobstore.containerName}, defaulted to
 * {@code archaiusProperties}.
 *
 * <h3>Connection</h3>
 *
 * To connect to, for example, {@code aws-s3} and route jclouds logging to SLF4J:
 *
 * <br/>
 * <br/>
 *
 * Load the following additional dependencies:
 * <pre>
 * org.jclouds.provider:aws-s3
 * org.jclouds:jclouds-slf4j
 * </pre>
 *
 * Create a context and pass it to the constructor:
 *
 * <pre>
 * context = ContextBuilder.newBuilder(&quot;aws-s3&quot;)
 *                         .credentials(apikey, secret)
 *                         .name("adrian-on-s3")
 *                         .modules(ImmutableSet.&lt;Module&gt; of(new SLF4JLoggingModule()))
 *                         .buildView(BlobStoreContext.class);
 *
 * configSource = new BlobStoreConfigurationSource(context);
 * </pre>
 *
 * <h3>Note</h3>
 *
 * <ul>
 * <li>This config source does not close the {@link BlobStoreContext}</li>
 * <li>Null config entries are logged to debug and ignored</li>
 * <li>log entries are prefixed with the value of {@link ContextBuilder#name}</li>
 * </ul>
 *
 * @see ContextBuilder
 * @author Adrian Cole
 */
public class BlobStoreConfigurationSource implements PolledConfigurationSource {
   private static final Logger log = LoggerFactory.getLogger(BlobStoreConfigurationSource.class);

   private DynamicStringProperty containerName = DynamicPropertyFactory.getInstance().getStringProperty(
         "com.netflix.config.blobstore.containerName", "archaiusProperties");

   private final BlobStoreContext ctx;

   public BlobStoreConfigurationSource(BlobStoreContext ctx) {
      this.ctx = checkNotNull(ctx, "ctx");
      String container = containerName.get();
      checkState(ctx.getBlobStore().containerExists(container), "%s: container %s doesn't exist", ctx.unwrap()
            .getName(), container);
   }

   @Override
   public PollResult poll(boolean initial, Object checkPoint) throws Exception {
      String container = containerName.get();
      BlobMap blobs = ctx.createBlobMap(container);
      Map<String, Object> props = ImmutableMap.copyOf(filterValues(transformEntries(blobs, toStringOrNull), notNull()));
      log.info("{}: {} properties in container {}", new Object[] { ctx.unwrap().getName(), props.size(), container });
      return PollResult.createFull(props);
   }

   private final EntryTransformer<String, Blob, Object> toStringOrNull = new EntryTransformer<String, Blob, Object>() {
      @Override
      public Object transformEntry(String key, Blob in) {
         String container = containerName.get();
         if (in == null || in.getPayload() == null) {
            log.warn("{}: null value for {}/{}", new Object[] { ctx.unwrap().getName(), container, key });
            return null;
         }
         try {
            return toStringAndClose(in.getPayload().getInput());
         } catch (IOException e) {
            log.warn("{}: error reading {}/{}: {}",
                  new Object[] { ctx.unwrap().getName(), container, key, e.getMessage() });
            return null;
         }
      }

   };

}
