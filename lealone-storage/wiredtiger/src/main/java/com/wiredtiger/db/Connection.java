/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 2.0.4
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package com.wiredtiger.db;

/**
  * @copydoc WT_CONNECTION
  * @ingroup wt_java
  */
public class Connection {
    private long swigCPtr;
    protected boolean swigCMemOwn;

    protected Connection(long cPtr, boolean cMemoryOwn) {
        swigCMemOwn = cMemoryOwn;
        swigCPtr = cPtr;
        wiredtigerJNI.Connection_java_init(swigCPtr, this, this);
    }

    protected static long getCPtr(Connection obj) {
        return (obj == null) ? 0 : obj.swigCPtr;
    }

    public synchronized void delete() {
        if (swigCPtr != 0) {
            if (swigCMemOwn) {
                swigCMemOwn = false;
                throw new UnsupportedOperationException("C++ destructor does not have public access");
            }
            swigCPtr = 0;
        }
    }

    /**
     * @copydoc WT_CONNECTION::async_flush
     */
    public int async_flush() throws com.wiredtiger.db.WiredTigerException {
        return wiredtigerJNI.Connection_async_flush(swigCPtr, this);
    }

    /**
     * @copydoc WT_CONNECTION::close
     */
    public int close(String config) throws com.wiredtiger.db.WiredTigerException {
        return wiredtigerJNI.Connection_close(swigCPtr, this, config);
    }

    /**
     * @copydoc WT_CONNECTION::reconfigure
     */
    public int reconfigure(String config) throws com.wiredtiger.db.WiredTigerException {
        return wiredtigerJNI.Connection_reconfigure(swigCPtr, this, config);
    }

    public String get_home() {
        return wiredtigerJNI.Connection_get_home(swigCPtr, this);
    }

    /**
     * @copydoc WT_CONNECTION::configure_method
     */
    public int configure_method(String method, String uri, String config, String type, String check)
            throws com.wiredtiger.db.WiredTigerException {
        return wiredtigerJNI.Connection_configure_method(swigCPtr, this, method, uri, config, type, check);
    }

    /**
     * @copydoc WT_CONNECTION::is_new
     */
    public int is_new() {
        return wiredtigerJNI.Connection_is_new(swigCPtr, this);
    }

    /**
     * @copydoc WT_CONNECTION::load_extension
     */
    public int load_extension(String path, String config) throws com.wiredtiger.db.WiredTigerException {
        return wiredtigerJNI.Connection_load_extension(swigCPtr, this, path, config);
    }

    protected int java_init(Object jsess) {
        return wiredtigerJNI.Connection_java_init(swigCPtr, this, jsess);
    }

    /**
     * @copydoc WT_CONNECTION::async_new_op
     */
    public AsyncOp async_new_op(String uri, String config, Object callbackObject)
            throws com.wiredtiger.db.WiredTigerException {
        long cPtr = wiredtigerJNI.Connection_async_new_op(swigCPtr, this, uri, config, callbackObject);
        return (cPtr == 0) ? null : new AsyncOp(cPtr, false);
    }

    /**
     * @copydoc WT_CONNECTION::open_session
     */
    public Session open_session(String config) throws com.wiredtiger.db.WiredTigerException {
        long cPtr = wiredtigerJNI.Connection_open_session(swigCPtr, this, config);
        return (cPtr == 0) ? null : new Session(cPtr, false);
    }

}
