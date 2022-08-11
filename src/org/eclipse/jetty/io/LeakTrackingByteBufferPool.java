//
// ========================================================================
// Copyright (c) 1995-2022 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v. 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
// which is available at https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.io;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.LeakDetector;
import org.eclipse.jetty.util.Logger;
import org.eclipse.jetty.util.component.ContainerLifeCycle;

public class LeakTrackingByteBufferPool extends ContainerLifeCycle implements ByteBufferPool
{
    private static final String TAG = LeakTrackingByteBufferPool.class.getSimpleName();

    private final LeakDetector<ByteBuffer> leakDetector = new LeakDetector<ByteBuffer>()
    {
        @Override
        public String id(ByteBuffer resource)
        {
            return BufferUtil.toIDString(resource);
        }

        @Override
        protected void leaked(LeakInfo leakInfo)
        {
            leaked.incrementAndGet();
            LeakTrackingByteBufferPool.this.leaked(leakInfo);
        }
    };

    private final AtomicLong leakedAcquires = new AtomicLong(0);
    private final AtomicLong leakedReleases = new AtomicLong(0);
    private final AtomicLong leakedRemoves = new AtomicLong(0);
    private final AtomicLong leaked = new AtomicLong(0);
    private final ByteBufferPool delegate;

    public LeakTrackingByteBufferPool(ByteBufferPool delegate)
    {
        this.delegate = delegate;
        addBean(leakDetector);
        addBean(delegate);
    }

    @Override
    public RetainableByteBufferPool asRetainableByteBufferPool()
    {
        // the retainable pool is just a client of the normal pool, so no special handling required.
        return delegate.asRetainableByteBufferPool();
    }

    @Override
    public ByteBuffer acquire(int size, boolean direct)
    {
        ByteBuffer buffer = delegate.acquire(size, direct);
        boolean acquired = leakDetector.acquired(buffer);
        if (!acquired)
        {
            leakedAcquires.incrementAndGet();
            if (Logger.isDebugEnabled())
                Logger.debug("ByteBuffer leaked acquire for id {}", leakDetector.id(buffer), new Throwable("acquire"));
        }
        return buffer;
    }

    @Override
    public void release(ByteBuffer buffer)
    {
        if (buffer == null)
            return;
        boolean released = leakDetector.released(buffer);
        if (!released)
        {
            leakedReleases.incrementAndGet();
            if (Logger.isDebugEnabled())
                Logger.debug("ByteBuffer leaked release for id {}", leakDetector.id(buffer), new Throwable("release"));
        }
        delegate.release(buffer);
    }

    @Override
    public void remove(ByteBuffer buffer)
    {
        if (buffer == null)
            return;
        boolean released = leakDetector.released(buffer);
        if (!released)
        {
            leakedRemoves.incrementAndGet();
            if (Logger.isDebugEnabled())
                Logger.debug("ByteBuffer leaked remove for id {}", leakDetector.id(buffer), new Throwable("remove"));
        }
        delegate.remove(buffer);
    }

    /**
     * Clears the tracking data returned by {@link #getLeakedAcquires()},
     * {@link #getLeakedReleases()}, {@link #getLeakedResources()}.
     */
    public void clearTracking()
    {
        leakedAcquires.set(0);
        leakedReleases.set(0);
    }

    /**
     * @return count of ByteBufferPool.acquire() calls that detected a leak
     */
    public long getLeakedAcquires()
    {
        return leakedAcquires.get();
    }

    /**
     * @return count of ByteBufferPool.release() calls that detected a leak
     */
    public long getLeakedReleases()
    {
        return leakedReleases.get();
    }

    /**
     * @return count of ByteBufferPool.remove() calls that detected a leak
     */
    public long getLeakedRemoves()
    {
        return leakedRemoves.get();
    }

    /**
     * @return count of resources that were acquired but not released
     */
    public long getLeakedResources()
    {
        return leaked.get();
    }

    protected void leaked(LeakDetector<ByteBuffer>.LeakInfo leakInfo)
    {
        Logger.warn("ByteBuffer {} leaked at: {}", leakInfo.getResourceDescription(), leakInfo.getStackFrames());
    }
}
