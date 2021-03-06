/*
 * Copyright 2019 GridGain Systems, Inc. and Contributors.
 *
 * Licensed under the GridGain Community Edition License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gridgain.com/products/software/community-edition/gridgain-community-edition-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.rest.protocols.tcp.redis;

/**
 * Supported Redis-specific commands.
 * See <a href="http://redis.io/commands">Redis commands</a> for details.
 * <p>
 * Cache operations are handled via REST.
 */
public enum GridRedisCommand {
    // Connections.
    /** Ping. */
    PING("PING"),
    /** Connection close. */
    QUIT("QUIT"),
    /** Echo. */
    ECHO("ECHO"),
    /** Select **/
    SELECT("SELECT"),

    // String commands.
    /** GET. */
    GET("GET"),
    /** MGET. */
    MGET("MGET"),
    /** SET. */
    SET("SET"),
    /** MSET. */
    MSET("MSET"),
    /** INCR. */
    INCR("INCR"),
    /** DECR. */
    DECR("DECR"),
    /** INCRBY. */
    INCRBY("INCRBY"),
    /** DECRBY. */
    DECRBY("DECRBY"),
    /** APPEND. */
    APPEND("APPEND"),
    /** STRLEN. */
    STRLEN("STRLEN"),
    /** GETSET. */
    GETSET("GETSET"),
    /** SETRANGE. */
    SETRANGE("SETRANGE"),
    /** GETRANGE. */
    GETRANGE("GETRANGE"),

    // Key commands.
    /** DEL. */
    DEL("DEL"),
    /** EXISTS. */
    EXISTS("EXISTS"),
    /** EXPIRE. */
    EXPIRE("EXPIRE"),
    /** PEXPIRE. */
    PEXPIRE("PEXPIRE"),

    // Server commands.
    /** DBSIZE. */
    DBSIZE("DBSIZE"),
    /** FLUSHDB. */
    FLUSHDB("FLUSHDB"),
    /** FLUSHALL. */
    FLUSHALL("FLUSHALL");

    /** String for command. */
    private final String cmd;

    /** Constructor. */
    GridRedisCommand(String cmd) {
        this.cmd = cmd;
    }
}
