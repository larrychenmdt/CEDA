/*
 *
 *  * Copyright 2003-2022 Beijing XinRong Meridian Limited.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * https://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package com.adaptiveMQ.server.transport.aeron;

import io.aeron.archive.Archive;

/**
 * Threading mode to be employed by the {@link org.agrona.concurrent.Agent}s in the {@link Archive}.
 */
public enum AeronThreadingMode
{
    /**
     * No threads are started in the {@link Archive}.
     * <p>
     * The {@link Archive} will be made runnable via an {@link Archive#invoker()}
     */
    INVOKER,

    /**
     * One thread shared by all {@link Archive} {@link org.agrona.concurrent.Agent}s.
     */
    SHARED,

    /**
     * 3 Threads, one dedicated to each of the {@link org.agrona.concurrent.Agent}s.
     */
    DEDICATED
}
