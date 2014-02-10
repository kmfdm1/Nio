/*
 * Copyright 2014 Manfred Andres
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * This interface must be implemented by those who war interested in receiving messages
 */
package org.freecs.nio.messagingServer;

public interface IMessageReceiver {
    /**
     * Gets called when added to MessagingHandler as callback and a message has
     * fully arrived
     * @param strg The message which arrived
     */
    public void receive(String strg);
}
