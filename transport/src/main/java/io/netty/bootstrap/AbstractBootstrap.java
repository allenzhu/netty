/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.netty.bootstrap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.util.AttributeKey;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link AbstractBootstrap} is a helper class that makes it easy to bootstrap a {@link Channel}. It support
 * method-chaining to provide an easy way to configure the {@link AbstractBootstrap}.
 */
abstract class AbstractBootstrap<B extends AbstractBootstrap<B, C>, C extends Channel> implements Cloneable {

    private volatile EventLoopGroup group;
    private volatile ChannelFactory<? extends C> channelFactory;
    private volatile SocketAddress localAddress;
    private final Map<ChannelOption<?>, Object> options = new LinkedHashMap<ChannelOption<?>, Object>();
    private final Map<AttributeKey<?>, Object> attrs = new LinkedHashMap<AttributeKey<?>, Object>();
    private volatile ChannelHandler handler;

    AbstractBootstrap() {
        // Disallow extending from a different package.
    }

    AbstractBootstrap(AbstractBootstrap<B, C> bootstrap) {
        group = bootstrap.group;
        channelFactory = bootstrap.channelFactory;
        handler = bootstrap.handler;
        localAddress = bootstrap.localAddress;
        synchronized (bootstrap.options) {
            options.putAll(bootstrap.options);
        }
        synchronized (bootstrap.attrs) {
            attrs.putAll(bootstrap.attrs);
        }
    }

    /**
     * The {@link EventLoopGroup} which is used to handle all the events for the to-be-creates
     * {@link Channel}
     */
    @SuppressWarnings("unchecked")
    public B group(EventLoopGroup group) {
        if (group == null) {
            throw new NullPointerException("group");
        }
        if (this.group != null) {
            throw new IllegalStateException("group set already");
        }
        this.group = group;
        return (B) this;
    }

    /**
     * The {@link Class} which is used to create {@link Channel} instances from.
     * You either use this or {@link #channelFactory(ChannelFactory)} if your
     * {@link Channel} implementation has no no-args constructor.
     */
    public B channel(Class<? extends C> channelClass) {
        if (channelClass == null) {
            throw new NullPointerException("channelClass");
        }
        return channelFactory(new BootstrapChannelFactory<C>(channelClass));
    }

    /**
     * {@link ChannelFactory} which is used to create {@link Channel} instances from
     * when calling {@link #bind()}. This method is usually only used if {@link #channel(Class)}
     * is not working for you because of some more complex needs. If your {@link Channel} implementation
     * has a no-args constructor, its highly recommend to just use {@link #channel(Class)} for
     * simplify your code.
     */
    @SuppressWarnings("unchecked")
    public B channelFactory(ChannelFactory<? extends C> channelFactory) {
        if (channelFactory == null) {
            throw new NullPointerException("channelFactory");
        }
        if (this.channelFactory != null) {
            throw new IllegalStateException("channelFactory set already");
        }

        this.channelFactory = channelFactory;
        return (B) this;
    }

    /**
     * The {@link SocketAddress} which is used to bind the local "end" to.
     *
     */
    @SuppressWarnings("unchecked")
    public B localAddress(SocketAddress localAddress) {
        this.localAddress = localAddress;
        return (B) this;
    }

    /**
     * @see {@link #localAddress(SocketAddress)}
     */
    public B localAddress(int inetPort) {
        return localAddress(new InetSocketAddress(inetPort));
    }

    /**
     * @see {@link #localAddress(SocketAddress)}
     */
    public B localAddress(String inetHost, int inetPort) {
        return localAddress(new InetSocketAddress(inetHost, inetPort));
    }

    /**
     * @see {@link #localAddress(SocketAddress)}
     */
    public B localAddress(InetAddress inetHost, int inetPort) {
        return localAddress(new InetSocketAddress(inetHost, inetPort));
    }

    /**
     * Allow to specify a {@link ChannelOption} which is used for the {@link Channel} instances once they got
     * created. Use a value of {@code null} to remove a previous set {@link ChannelOption}.
     */
    @SuppressWarnings("unchecked")
    public <T> B option(ChannelOption<T> option, T value) {
        if (option == null) {
            throw new NullPointerException("option");
        }
        if (value == null) {
            synchronized (options) {
                options.remove(option);
            }
        } else {
            synchronized (options) {
                options.put(option, value);
            }
        }
        return (B) this;
    }

    /**
     * Allow to specify an initial attribute of the newly created {@link Channel}.  If the {@code value} is
     * {@code null}, the attribute of the specified {@code key} is removed.
     */
    public <T> B attr(AttributeKey<T> key, T value) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (value == null) {
            synchronized (attrs) {
                attrs.remove(key);
            }
        } else {
            synchronized (attrs) {
                attrs.put(key, value);
            }
        }

        @SuppressWarnings("unchecked")
        B b = (B) this;
        return b;
    }

    /**
     * Shutdown the {@link AbstractBootstrap} and the {@link EventLoopGroup} which is
     * used by it. Only call this if you don't share the {@link EventLoopGroup}
     * between different {@link AbstractBootstrap}'s.
     */
    public void shutdown() {
        if (group != null) {
            group.shutdown();
        }
    }

    /**
     * Validate all the parameters. Sub-classes may override this, but should
     * call the super method in that case.
     */
    @SuppressWarnings("unchecked")
    public B validate() {
        if (group == null) {
            throw new IllegalStateException("group not set");
        }
        if (channelFactory == null) {
            throw new IllegalStateException("factory not set");
        }
        return (B) this;
    }

    /**
     * Returns a deep clone of this bootstrap which has the identical configuration.  This method is useful when making
     * multiple {@link Channel}s with similar settings.  Please note that this method does not clone the
     * {@link EventLoopGroup} deeply but shallowly, making the group a shared resource.
     */
    @Override
    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    public abstract B clone();

    /**
     * Create a new {@link Channel} and bind it.
     */
    public ChannelFuture bind() {
        validate();
        SocketAddress localAddress = this.localAddress;
        if (localAddress == null) {
            throw new IllegalStateException("localAddress not set");
        }
        return doBind(localAddress);
    }

    /**
     * Create a new {@link Channel} and bind it.
     */
    public ChannelFuture bind(int inetPort) {
        return bind(new InetSocketAddress(inetPort));
    }

    /**
     * Create a new {@link Channel} and bind it.
     */
    public ChannelFuture bind(String inetHost, int inetPort) {
        return bind(new InetSocketAddress(inetHost, inetPort));
    }

    /**
     * Create a new {@link Channel} and bind it.
     */
    public ChannelFuture bind(InetAddress inetHost, int inetPort) {
        return bind(new InetSocketAddress(inetHost, inetPort));
    }

    /**
     * Create a new {@link Channel} and bind it.
     */
    public ChannelFuture bind(SocketAddress localAddress) {
        validate();
        if (localAddress == null) {
            throw new NullPointerException("localAddress");
        }
        return doBind(localAddress);
    }

    abstract ChannelFuture doBind(SocketAddress localAddress);

    /**
     * the {@link ChannelHandler} to use for serving the requests.
     */
    @SuppressWarnings("unchecked")
    public B handler(ChannelHandler handler) {
        if (handler == null) {
            throw new NullPointerException("handler");
        }
        this.handler = handler;
        return (B) this;
    }

    final SocketAddress localAddress() {
        return localAddress;
    }

    final ChannelFactory<? extends C> channelFactory() {
        return channelFactory;
    }

    final ChannelHandler handler() {
        return handler;
    }

    final EventLoopGroup group() {
        return group;
    }

    final Map<ChannelOption<?>, Object> options() {
        return options;
    }

    final Map<AttributeKey<?>, Object> attrs() {
        return attrs;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(getClass().getSimpleName());
        buf.append('(');
        if (group != null) {
            buf.append("group: ");
            buf.append(group.getClass().getSimpleName());
            buf.append(", ");
        }
        if (channelFactory != null) {
            buf.append("channelFactory: ");
            buf.append(channelFactory);
            buf.append(", ");
        }
        if (localAddress != null) {
            buf.append("localAddress: ");
            buf.append(localAddress);
            buf.append(", ");
        }
        synchronized (options) {
            if (!options.isEmpty()) {
                buf.append("options: ");
                buf.append(options);
                buf.append(", ");
            }
        }
        synchronized (attrs) {
            if (!attrs.isEmpty()) {
                buf.append("attrs: ");
                buf.append(attrs);
                buf.append(", ");
            }
        }
        if (handler != null) {
            buf.append("handler: ");
            buf.append(handler);
            buf.append(", ");
        }
        if (buf.charAt(buf.length() - 1) == '(') {
            buf.append(')');
        } else {
            buf.setCharAt(buf.length() - 2, ')');
            buf.setLength(buf.length() - 1);
        }
        return buf.toString();
    }

    private static final class BootstrapChannelFactory<T extends Channel> implements ChannelFactory<T> {
        private final Class<? extends T> clazz;

        BootstrapChannelFactory(Class<? extends T> clazz) {
            this.clazz = clazz;
        }

        @Override
        public T newChannel() {
            try {
                return clazz.newInstance();
            } catch (Throwable t) {
                throw new ChannelException("Unable to create Channel from class " + clazz, t);
            }
        }

        @Override
        public String toString() {
            return clazz.getSimpleName() + ".class";
        }
    }
}
