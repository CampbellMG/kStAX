package com.cmgcode.xml.xmlpull

/**
 * Define an interface to serialization of XML Infoset.
 * This interface abstracts away if serialized XML is XML 1.0 compatible text or
 * other formats of XML 1.0 serializations (such as binary XML for example with WBXML).
 *
 *
 * **PLEASE NOTE:** This interface will be part of XmlPull 1.2 API.
 * It is included as basis for discussion. It may change in any way.
 *
 *
 * Exceptions that may be thrown are: IOException or runtime exception
 * (more runtime exceptions can be thrown but are not declared and as such
 * have no semantics defined for this interface):
 *
 *  * *IllegalArgumentException* - for almost all methods to signal that
 * argument is illegal
 *  * *IllegalStateException* - to signal that call has good arguments but
 * is not expected here (violation of contract) and for features/properties
 * when requesting setting unimplemented feature/property
 * (UnsupportedOperationException would be better but it is not in MIDP)
 *
 *
 *
 * **NOTE:** writing  CDSECT, ENTITY_REF, IGNORABLE_WHITESPACE,
 * PROCESSING_INSTRUCTION, COMMENT, and DOCDECL in some implementations
 * may not be supported (for example when serializing to WBXML).
 * In such case IllegalStateException will be thrown and it is recommended
 * to use an optional feature to signal that implementation is not
 * supporting this kind of output.
 */

interface XmlSerializer {

    /**
     * Returns the current depth of the element.
     * Outside the root element, the depth is 0. The
     * depth is incremented by 1 when startTag() is called.
     * The depth is decremented after the call to endTag()
     * event was observed.
     *
     * <pre>
     * &lt;!-- outside --&gt;     0
     * &lt;root&gt;               1
     * sometext                 1
     * &lt;foobar&gt;         2
     * &lt;/foobar&gt;        2
     * &lt;/root&gt;              1
     * &lt;!-- outside --&gt;     0
    </pre> *
     */
    val depth: Int

    /**
     * Returns the namespace URI of the current element as set by startTag().
     *
     *
     * **NOTE:** that means in particular that:
     *  * if there was startTag("", ...) then getNamespace() returns ""
     *  * if there was startTag(null, ...) then getNamespace() returns null
     *
     *
     * @return namespace set by startTag() that is currently in scope
     */
    val namespace: String

    /**
     * Returns the name of the current element as set by startTag().
     * It can only be null before first call to startTag()
     * or when last endTag() is called to close first startTag().
     *
     * @return namespace set by startTag() that is currently in scope
     */
    val name: String

    /**
     * Set feature identified by name (recommended to be URI for uniqueness).
     * Some well known optional features are defined in
     * [
 * http://www.xmlpull.org/io.v1/doc/features.html](http://www.xmlpull.org/io.v1/doc/features.html).
     *
     * If feature is not recognized or can not be set
     * then IllegalStateException MUST be thrown.
     *
     * @exception IllegalStateException If the feature is not supported or can not be set
     */
    fun setFeature(name: String,
                   state: Boolean)


    /**
     * Return the current value of the feature with given name.
     *
     * **NOTE:** unknown properties are **always** returned as null
     *
     * @param name The name of feature to be retrieved.
     * @return The value of named feature.
     * @exception IllegalArgumentException if feature string is null
     */
    fun getFeature(name: String): Boolean


    /**
     * Set the value of a property.
     * (the property name is recommended to be URI for uniqueness).
     * Some well known optional properties are defined in
     * [
 * http://www.xmlpull.org/io.v1/doc/properties.html](http://www.xmlpull.org/io.v1/doc/properties.html).
     *
     * If property is not recognized or can not be set
     * then IllegalStateException MUST be thrown.
     *
     * @exception IllegalStateException if the property is not supported or can not be set
     */
    fun setProperty(name: String,
                    value: Any)

    /**
     * Look up the value of a property.
     *
     * The property name is any fully-qualified URI. I
     *
     * **NOTE:** unknown properties are <string>always returned as null
     *
     * @param name The name of property to be retrieved.
     * @return The value of named property.
    </string> */
    fun getProperty(name: String): Any

    /**
     * Set to use binary output stream with given encoding.
     */
//    fun setOutput(os: OutputStream, encoding: String) - From Java implementation
    fun setOutput(os: String)
    /**
     * Set the output to the given writer.
     *
     * **WARNING** no information about encoding is available!
     */
    //fun setOutput(writer: Writer) - From Java Implementation

    /**
     * Write &lt;&#63;xml declaration with encoding (if encoding not null)
     * and standalone flag (if standalone not null)
     * This method can only be called just after setOutput.
     */
    fun startDocument(encoding: String, standalone: Boolean?)

    /**
     * Finish writing. All unclosed start tags will be closed and output
     * will be flushed. After calling this method no more output can be
     * serialized until next call to setOutput()
     */
    fun endDocument()

    /**
     * Binds the given prefix to the given namespace.
     * This call is valid for the next element including child elements.
     * The prefix and namespace MUST be always declared even if prefix
     * is not used in element (startTag() or attribute()) - for XML 1.0
     * it must result in declaring `xmlns:prefix='namespace'`
     * (or `xmlns:prefix="namespace"` depending what character is used
     * to quote attribute value).
     *
     *
     * **NOTE:** this method MUST be called directly before startTag()
     * and if anything but startTag() or setPrefix() is called next there will be exception.
     *
     * **NOTE:** prefixes "xml" and "xmlns" are already bound
     * and can not be redefined see:
     * [Namespaces in XML Errata](http://www.w3.org/XML/xml-names-19990114-errata#NE05).
     *
     * **NOTE:** to set default namespace use as prefix empty string.
     *
     * @param prefix must be not null (or IllegalArgumentException is thrown)
     * @param namespace must be not null
     */
    fun setPrefix(prefix: String, namespace: String)

    /**
     * Return namespace that corresponds to given prefix
     * If there is no prefix bound to this namespace return null
     * but if generatePrefix is false then return generated prefix.
     *
     *
     * **NOTE:** if the prefix is empty string "" and default namespace is bound
     * to this prefix then empty string ("") is returned.
     *
     *
     * **NOTE:** prefixes "xml" and "xmlns" are already bound
     * will have values as defined
     * [Namespaces in XML specification](http://www.w3.org/TR/REC-xml-names/)
     */
    fun getPrefix(namespace: String, generatePrefix: Boolean): String

    /**
     * Writes a start tag with the given namespace and name.
     * If there is no prefix defined for the given namespace,
     * a prefix will be defined automatically.
     * The explicit prefixes for namespaces can be established by calling setPrefix()
     * immediately before this method.
     * If namespace is null no namespace prefix is printed but just name.
     * If namespace is empty string then serializer will make sure that
     * default empty namespace is declared (in XML 1.0 xmlns='')
     * or throw IllegalStateException if default namespace is already bound
     * to non-empty string.
     */
    fun startTag(namespace: String, name: String): XmlSerializer

    /**
     * Write an attribute. Calls to attribute() MUST follow a call to
     * startTag() immediately. If there is no prefix defined for the
     * given namespace, a prefix will be defined automatically.
     * If namespace is null or empty string
     * no namespace prefix is printed but just name.
     */
    fun attribute(namespace: String, name: String, value: String): XmlSerializer

    /**
     * Write end tag. Repetition of namespace and name is just for avoiding errors.
     *
     * **Background:** in kXML endTag had no arguments, and non matching tags were
     * very difficult to find...
     * If namespace is null no namespace prefix is printed but just name.
     * If namespace is empty string then serializer will make sure that
     * default empty namespace is declared (in XML 1.0 xmlns='').
     */
    fun endTag(namespace: String, name: String): XmlSerializer


    //    /**
    //     * Writes a start tag with the given namespace and name.
    //     * <br />If there is no prefix defined (prefix == null) for the given namespace,
    //     * a prefix will be defined automatically.
    //     * <br />If explicit prefixes is passed (prefix != null) then it will be used
    //      *and namespace declared if not already declared or
    //     * throw IllegalStateException the same prefix was already set on this
    //     * element (setPrefix()) and was bound to different namespace.
    //     * <br />If namespace is null then prefix must be null too or IllegalStateException is thrown.
    //     * <br />If namespace is null then no namespace prefix is printed but just name.
    //     * <br />If namespace is empty string then serializer will make sure that
    //     * default empty namespace is declared (in XML 1.0 xmlns='')
    //     * or throw IllegalStateException if default namespace is already bound
    //     * to non-empty string.
    //     */
    //    KXmlSerializer startTag (String prefix, String namespace, String name)
    //        throws IOException, IllegalArgumentException, IllegalStateException;
    //
    //    /**
    //     * Write an attribute. Calls to attribute() MUST follow a call to
    //     * startTag() immediately.
    //     * <br />If there is no prefix defined (prefix == null) for the given namespace,
    //     * a prefix will be defined automatically.
    //     * <br />If explicit prefixes is passed (prefix != null) then it will be used
    //     * and namespace declared if not already declared or
    //     * throw IllegalStateException the same prefix was already set on this
    //     * element (setPrefix()) and was bound to different namespace.
    //     * <br />If namespace is null then prefix must be null too or IllegalStateException is thrown.
    //     * <br />If namespace is null then no namespace prefix is printed but just name.
    //     * <br />If namespace is empty string then serializer will make sure that
    //     * default empty namespace is declared (in XML 1.0 xmlns='')
    //     * or throw IllegalStateException if default namespace is already bound
    //     * to non-empty string.
    //     */
    //    KXmlSerializer attribute (String prefix, String namespace, String name, String value)
    //        throws IOException, IllegalArgumentException, IllegalStateException;
    //
    //    /**
    //     * Write end tag. Repetition of namespace, prefix, and name is just for avoiding errors.
    //     * <br />If namespace or name arguments are different from corresponding startTag call
    //     * then IllegalArgumentException is thrown, if prefix argument is not null and is different
    //     * from corresponding starTag then IllegalArgumentException is thrown.
    //     * <br />If namespace is null then prefix must be null too or IllegalStateException is thrown.
    //     * <br />If namespace is null then no namespace prefix is printed but just name.
    //     * <br />If namespace is empty string then serializer will make sure that
    //     * default empty namespace is declared (in XML 1.0 xmlns='').
    //     * <p><b>Background:</b> in kXML endTag had no arguments, and non matching tags were
    //     *  very difficult to find...</p>
    //     */
    // ALEK: This is really optional as prefix in end tag MUST correspond to start tag but good for error checking
    //    KXmlSerializer endTag (String prefix, String namespace, String name)
    //        throws IOException, IllegalArgumentException, IllegalStateException;

    /**
     * Writes text, where special XML chars are escaped automatically
     */
    fun text(text: String): XmlSerializer

    /**
     * Writes text, where special XML chars are escaped automatically
     */
    fun text(buf: CharArray, start: Int, len: Int): XmlSerializer

    fun cdsect(text: String)

    fun entityRef(text: String)

    fun processingInstruction(text: String)

    fun comment(text: String)

    fun docdecl(text: String)

    fun ignorableWhitespace(text: String)

    /**
     * Write all pending output to the stream.
     * If method startTag() or attribute() was called then start tag is closed (final &gt;)
     * before flush() is called on underlying output stream.
     *
     *
     * **NOTE:** if there is need to close start tag
     * (so no more attribute() calls are allowed) but without flushing output
     * call method text() with empty string (text("")).
     *
     */
    fun flush()

}
