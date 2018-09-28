///* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
//// for license please see accompanying LICENSE.txt file (available also at http://www.xmlpull.org/)
//
//package io.v1
//
//import io.kotlin.xmlpull.KXmlPullParser
//
///**
// * This class is used to create implementations of XML Pull Parser defined in XMPULL V1 API.
// *
// * @see KXmlPullParser
// *
// *
// * @author [Aleksander Slominski](http://www.extreme.indiana.edu/~aslom/)
// * @author Stefan Haustein
// */
//
//class KXmlPullParserFactory
///**
// * Protected constructor to be called by factory implementations.
// */
//protected constructor() {
//    protected var parserClasses: ArrayList<*>? = null
//    protected var serializerClasses: ArrayList<*>? = null
//
//    /** Unused, but we have to keep it because it's public API.  */
//    protected var classNamesLocation: String? = null
//
//    // features are kept there
//    // TODO: This can't be made final because it's a public API.
//    protected var features = HashMap<String, Boolean>()
//
//    /**
//     * Indicates whether or not the factory is configured to produce
//     * parsers which are namespace aware
//     * (it simply set feature KXmlPullParser.FEATURE_PROCESS_NAMESPACES to true or false).
//     *
//     * @return  true if the factory is configured to produce parsers
//     * which are namespace aware; false otherwise.
//     */
//    /**
//     * Specifies that the parser produced by this factory will provide
//     * support for XML namespaces.
//     * By default the value of this is set to false.
//     *
//     * @param awareness true if the parser produced by this code
//     * will provide support for XML namespaces;  false otherwise.
//     */
//    var isNamespaceAware: Boolean
//        get() = getFeature(KXmlPullParser.FEATURE_PROCESS_NAMESPACES)
//        set(awareness) {
//            features[KXmlPullParser.FEATURE_PROCESS_NAMESPACES] = awareness
//        }
//
//    /**
//     * Indicates whether or not the factory is configured to produce parsers
//     * which validate the XML content during parse.
//     *
//     * @return   true if the factory is configured to produce parsers
//     * which validate the XML content during parse; false otherwise.
//     */
//
//    /**
//     * Specifies that the parser produced by this factory will be validating
//     * (it simply set feature KXmlPullParser.FEATURE_VALIDATION to true or false).
//     *
//     * By default the value of this is set to false.
//     *
//     * @param validating - if true the parsers created by this factory  must be validating.
//     */
//    var isValidating: Boolean
//        get() = getFeature(KXmlPullParser.FEATURE_VALIDATION)
//        set(validating) {
//            features[KXmlPullParser.FEATURE_VALIDATION] = validating
//        }
//
//    private val parserInstance: KXmlPullParser
//        get() {
//            var exceptions: ArrayList<Exception>? = null
//
//            if (parserClasses != null && !parserClasses!!.isEmpty()) {
//                exceptions = ArrayList()
//                for (o in parserClasses!!) {
//                    try {
//                        if (o != null) {
//                            val parserClass = o as Class<*>
//                            return parserClass.newInstance() as KXmlPullParser
//                        }
//                    } catch (e: InstantiationException) {
//                        exceptions.add(e)
//                    } catch (e: IllegalAccessException) {
//                        exceptions.add(e)
//                    } catch (e: ClassCastException) {
//                        exceptions.add(e)
//                    }
//
//                }
//            }
//
//            throw newInstantiationException("Invalid parser class list", exceptions)
//        }
//
//    private val serializerInstance: KXmlSerializer
//        get() {
//            var exceptions: ArrayList<Exception>? = null
//
//            if (serializerClasses != null && !serializerClasses!!.isEmpty()) {
//                exceptions = ArrayList()
//                for (o in serializerClasses!!) {
//                    try {
//                        if (o != null) {
//                            val serializerClass = o as Class<*>
//                            return serializerClass.newInstance() as KXmlSerializer
//                        }
//                    } catch (e: InstantiationException) {
//                        exceptions.add(e)
//                    } catch (e: IllegalAccessException) {
//                        exceptions.add(e)
//                    } catch (e: ClassCastException) {
//                        exceptions.add(e)
//                    }
//
//                }
//            }
//
//            throw newInstantiationException("Invalid serializer class list", exceptions)
//        }
//
//    init {
//        parserClasses = ArrayList<String>()
//        serializerClasses = ArrayList<String>()
//
//        try {
//            parserClasses!!.add(Class.forName("io.KXmlParser"))
//            serializerClasses!!.add(Class.forName("io.KXmlSerializer"))
//        } catch (e: ClassNotFoundException) {
//            throw AssertionError()
//        }
//
//    }
//
//    /**
//     * Set the features to be set when XML Pull Parser is created by this factory.
//     *
//     * **NOTE:** factory features are not used for XML Serializer.
//     *
//     * @param name string with URI identifying feature
//     * @param state if true feature will be set; if false will be ignored
//     */
//    fun setFeature(name: String, state: Boolean) {
//        features[name] = state
//    }
//
//
//    /**
//     * Return the current value of the feature with given name.
//     *
//     * **NOTE:** factory features are not used for XML Serializer.
//     *
//     * @param name The name of feature to be retrieved.
//     * @return The value of named feature.
//     * Unknown features are <string>always returned as false
//    </string> */
//    fun getFeature(name: String): Boolean {
//        val value = features[name]
//        return if (value != null) value.booleanValue() else false
//    }
//
//    /**
//     * Creates a new instance of a XML Pull Parser
//     * using the currently configured factory features.
//     *
//     * @return A new instance of a XML Pull Parser.
//     */
//    fun newPullParser(): KXmlPullParser {
//        val pp = parserInstance
//        for ((key, value) in features) {
//            // NOTE: This test is needed for compatibility reasons. We guarantee
//            // that we only set a feature on a parser if its value is true.
//            if (value) {
//                pp.setFeature(key, value)
//            }
//        }
//
//        return pp
//    }
//
//    /**
//     * Creates a new instance of a XML Serializer.
//     *
//     *
//     * **NOTE:** factory features are not used for XML Serializer.
//     *
//     * @return A new instance of a XML Serializer.
//     * @throws KXmlPullParserException if a parser cannot be created which satisfies the
//     * requested configuration.
//     */
//
//    fun newSerializer(): KXmlSerializer {
//        return serializerInstance
//    }
//
//    companion object {
//
//        val PROPERTY_NAME = "io.v1.KXmlPullParserFactory"
//
//        private fun newInstantiationException(message: String,
//                                              exceptions: ArrayList<Exception>?): KXmlPullParserException {
//            if (exceptions == null || exceptions.isEmpty()) {
//                return KXmlPullParserException(message)
//            } else {
//                val exception = KXmlPullParserException(message)
//                for (ex in exceptions) {
//                    exception.addSuppressed(ex)
//                }
//
//                return exception
//            }
//        }
//
//        /**
//         * Creates a new instance of a PullParserFactory that can be used
//         * to create XML pull parsers. The factory will always return instances
//         * of Android's built-in [KXmlPullParser] and [KXmlSerializer].
//         */
//        fun newInstance(): KXmlPullParserFactory {
//            return KXmlPullParserFactory()
//        }
//
//        /**
//         * Creates a factory that always returns instances of Android's built-in
//         * [KXmlPullParser] and [KXmlSerializer] implementation. This
//         * **does not** support factories capable of creating arbitrary parser
//         * and serializer implementations. Both arguments to this method are unused.
//         */
//        fun newInstance(unused: String, unused2: Class<*>): KXmlPullParserFactory {
//            return newInstance()
//        }
//    }
//}
