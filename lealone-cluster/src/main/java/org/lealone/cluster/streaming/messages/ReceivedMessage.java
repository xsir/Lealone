/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lealone.cluster.streaming.messages;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.lealone.cluster.io.DataInputPlus;
import org.lealone.cluster.io.DataInputPlus.DataInputStreamPlus;
import org.lealone.cluster.io.DataOutputStreamPlus;
import org.lealone.cluster.streaming.StreamSession;

public class ReceivedMessage extends StreamMessage {
    public static Serializer<ReceivedMessage> serializer = new Serializer<ReceivedMessage>() {
        @Override
        public ReceivedMessage deserialize(ReadableByteChannel in, int version, StreamSession session)
                throws IOException {
            DataInputPlus input = new DataInputStreamPlus(Channels.newInputStream(in));
            return new ReceivedMessage(input.readUTF(), input.readInt());
        }

        @Override
        public void serialize(ReceivedMessage message, DataOutputStreamPlus out, int version, StreamSession session)
                throws IOException {
            out.writeUTF(message.mapName);
            out.writeInt(message.sequenceNumber);
        }
    };

    public final String mapName;
    public final int sequenceNumber;

    public ReceivedMessage(String mapName, int sequenceNumber) {
        super(Type.RECEIVED);
        this.mapName = mapName;
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Received (");
        sb.append(mapName).append(", #").append(sequenceNumber).append(')');
        return sb.toString();
    }
}
