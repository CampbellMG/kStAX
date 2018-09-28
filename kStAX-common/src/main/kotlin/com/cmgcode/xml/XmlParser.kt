/* Copyright (c) 2002,2003, Stefan Haustein, Oberhausen, Rhld., Germany
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The  above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE. */

// Contributors: Paul Hackenberger (unterminated entity handling in relaxed mode)

package com.cmgcode.xml

import com.cmgcode.xml.xmlpull.XmlPullParser
import com.cmgcode.xml.xmlpull.XmlPullParserException

/** A simple, pull based XML parser. This classe replaces the kXML 1
 * XmlParser class and the corresponding event classes.  */

class XmlParser : XmlPullParser {

    private var location: Any? = null

    // general

    override val isEmptyElementTag: Boolean
        get() {
            if (eventType != XmlPullParser.START_TAG)
                exception(ILLEGAL_TYPE)
            return degenerated
        }

    override val text: String?
        get() {
            return when {
                eventType < XmlPullParser.TEXT || eventType == XmlPullParser.ENTITY_REF && unresolved -> null
                else -> get(0)
            }
        }

    override val positionDescription: String
        get() {
            val buf = StringBuilder()
            buf.append(' ')

            if (eventType == XmlPullParser.START_TAG || eventType == XmlPullParser.END_TAG) {
                if (degenerated)
                    buf.append("(empty) ")
                buf.append('<')
                if (eventType == XmlPullParser.END_TAG)
                    buf.append('/')

                buf.append("{$namespace}$prefix:")
                buf.append(name)

                val cnt = attributeCount shl 2
                var i = 0
                while (i < cnt) {
                    buf.append(' ')
                    if (attributes[i + 1] != null)
                        buf.append(
                                "{" + attributes[i] + "}" + attributes[i + 1] + ":")
                    buf.append(attributes[i + 2] + "='" + attributes[i + 3] + "'")
                    i += 4
                }

                buf.append('>')
            } else if (eventType == XmlPullParser.IGNORABLE_WHITESPACE)
            else if (eventType != XmlPullParser.TEXT)
                buf.append(text)
            else if (isWhitespace)
                buf.append("(whitespace)")
            else {
                var text = text
                if (text!!.length > 16)
                    text = text.substring(0, 16) + "..."
                buf.append(text)
            }

            buf.append("@$lineNumber:$columnNumber")
            if (location != null) {
                buf.append(" in ")
                buf.append(location)
            } else if (reader != null) {
                buf.append(" in ")
                buf.append(reader!!.toString())
            }
            return buf.toString()
        }

    private var version: String? = null
    private var standalone: Boolean? = null

    private var processNsp: Boolean = false
    private var relaxed: Boolean = false
    private var entityMap: MutableMap<String, String> = mutableMapOf()
    override var depth: Int = 0
    private var elementStack = arrayOfNulls<String>(16)
    private var nspStack = ArrayList<String>(8)
    private var nspCounts = IntArray(4)

    // source

    private var reader: CharIterator? = null
    override var inputEncoding: String = ""
    private val srcBuf: CharArray = CharArray(8192)

    private var srcPos: Int = 0
    private var srcCount: Int = 0

    override var lineNumber: Int = 0
    override var columnNumber: Int = 0

    // txtbuffer

    /** Target buffer for storing incoming text (including aggregated resolved entities)  */
    private var txtBuf = CharArray(128)
    /** Write position   */
    private var txtPos: Int = 0

    // Event-related

    override var eventType: Int = 0
    override var isWhitespace: Boolean = false
//        get() {
//            if (eventType != XmlPullParser.TEXT && eventType != XmlPullParser.IGNORABLE_WHITESPACE && eventType != XmlPullParser.CDSECT)
//                exception(ILLEGAL_TYPE)
//            return field
//        }

    override var namespace: String = ""
    override var prefix: String = ""
    override var name: String = ""

    private var degenerated: Boolean = false
    override var attributeCount: Int = 0
    private var attributes = arrayOfNulls<String>(16)
    //    private int stackMismatch = 0;
    private var error: String? = null

    /**
     * A separate peek buffer seems simpler than managing
     * wrap around in the first level read buffer  */

    private val peek = IntArray(2)
    private var peekCount: Int = 0
    private var wasCR: Boolean = false

    private var unresolved: Boolean = false
    private var token: Boolean = false

    private fun isProp(n1: String, prop: Boolean, n2: String): Boolean {
        if (!n1.startsWith("http://xmlpull.org/io.v1/doc/"))
            return false
        return if (prop)
            n1.substring(42) == n2
        else
            n1.substring(40) == n2
    }

    private fun adjustNsp(): Boolean {

        var any = false

        run {
            var i = 0
            while (i < attributeCount shl 2) {
                // * 4 - 4; i >= 0; i -= 4) {

                var attrName: String? = attributes[i + 2]
                val cut = attrName!!.indexOf(':')
                val prefix: String

                if (cut != -1) {
                    prefix = attrName!!.substring(0, cut)
                    attrName = attrName!!.substring(cut + 1)
                } else if (attrName == "xmlns") {
                    prefix = attrName!!
                    attrName = null
                } else {
                    i += 4
                    continue
                }

                if (prefix != "xmlns") {
                    any = true
                } else {
                    val j = nspCounts[depth]++ shl 1

                    nspStack[j] = attrName!!
                    nspStack[j + 1] = attributes[i + 3]!!

                    if (attrName != null && attributes[i + 3] == "")
                        error("illegal empty namespace")

                    //  prefixMap = new PrefixMap (prefixMap, attrName, attr.getValue ());

                    //System.out.println (prefixMap);

                    attributes = attributes.copyOfRange(0, i) + attributes.copyOfRange(i + 4, (i + 4) + (--attributeCount shl 2) - i)


                    i -= 4
                }
                i += 4
            }
        }

        if (any) {
            var i = (attributeCount shl 2) - 4
            while (i >= 0) {

                var attrName = attributes[i + 2]
                val cut = attrName?.indexOf(':')

                if (cut == 0 && !relaxed)
                    throw RuntimeException(
                            "illegal attribute name: " + attrName + " at " + this)
                else if (cut != -1) {
                    val attrPrefix = attrName?.substring(0, cut!!)

                    attrName = attrName?.substring(cut!! + 1)

                    val attrNs = getNamespace(attrPrefix!!)

                    if (!relaxed)
                        throw RuntimeException(
                                "Undefined Prefix: " + attrPrefix + " in " + this)

                    attributes[i] = attrNs
                    attributes[i + 1] = attrPrefix
                    attributes[i + 2] = attrName

                    /*
                                        if (!relaxed) {
                                            for (int j = (attributeCount << 2) - 4; j > i; j -= 4)
                                                if (attrName.equals(attributes[j + 2])
                                                    && attrNs.equals(attributes[j]))
                                                    exception(
                                                        "Duplicate Attribute: {"
                                                            + attrNs
                                                            + "}"
                                                            + attrName);
                                        }
                        */
                }
                i -= 4
            }
        }

        val cut = name.indexOf(':')

        if (cut == 0)
            error("illegal tag name: $name")

        if (cut != -1) {
            prefix = name.substring(0, cut)
            name = name.substring(cut + 1)
        }

        this.namespace = getNamespace(prefix)

        return any
    }

    private fun ensureCapacity(arr: Array<String?>, required: Int): Array<String?> {
        if (arr.size >= required)
            return arr
        return arr.copyOf(required + 16)
    }

    private fun error(desc: String) {
        if (relaxed) {
            if (error == null)
                error = "ERR: $desc"
        } else
            exception(desc)
    }

    private fun exception(desc: String) {
        throw XmlPullParserException(
                if (desc.length < 100) desc else desc.substring(0, 100) + "\n",
                this, null)
    }

    /**
     * common base for next and nextToken. Clears the state, except from
     * txtPos and whitespace. Does not set the eventType variable  */

    private fun nextImpl() {

        if (reader == null)
            exception("No Input specified")

        if (eventType == XmlPullParser.END_TAG)
            depth--

        while (true) {
            attributeCount = -1

            // degenerated needs to be handled before error because of possible
            // processor expectations(!)

            if (degenerated) {
                degenerated = false
                eventType = XmlPullParser.END_TAG
                return
            }


            if (error != null) {
                for (i in 0 until error!!.length)
                    push(error!![i].toInt())
                //				text = error;
                error = null
                eventType = XmlPullParser.COMMENT
                return
            }


            //            if (relaxed
            //                && (stackMismatch > 0 || (peek(0) == -1 && depth > 0))) {
            //                int sp = (depth - 1) << 2;
            //                eventType = END_TAG;
            //                namespace = elementStack[sp];
            //                prefix = elementStack[sp + 1];
            //                name = elementStack[sp + 2];
            //                if (stackMismatch != 1)
            //                    error = "missing end tag /" + name + " inserted";
            //                if (stackMismatch > 0)
            //                    stackMismatch--;
            //                return;
            //            }

//            prefix = null
//            name = null
//            namespace = null
            //            text = null;

            eventType = peekType()

            when (eventType) {

                XmlPullParser.ENTITY_REF -> {
                    pushEntity()
                    return
                }

                XmlPullParser.START_TAG -> {
                    parseStartTag(false)
                    return
                }

                XmlPullParser.END_TAG -> {
                    parseEndTag()
                    return
                }

                XmlPullParser.END_DOCUMENT -> return

                XmlPullParser.TEXT -> {
                    pushText('<'.toInt(), !token)
                    if (depth == 0) {
                        if (isWhitespace)
                            eventType = XmlPullParser.IGNORABLE_WHITESPACE
                        // make exception switchable for instances.chg... !!!!
                        //	else
                        //    exception ("text '"+getText ()+"' not allowed outside root element");
                    }
                    return
                }

                else -> {
                    eventType = parseLegacy(token)
                    if (eventType != XML_DECL)
                        return
                }
            }
        }
    }

    private fun parseLegacy(pushVar: Boolean): Int {
        var push = pushVar

        var req = ""
        val term: Int
        val result: Int
        var prev = 0

        read() // <
        var c = read()

        when (c) {
            '?'.toInt() -> {
                if ((peek(0) == 'x'.toInt() || peek(0) == 'X'.toInt()) && (peek(1) == 'm'.toInt() || peek(1) == 'M'.toInt())) {

                    if (push) {
                        push(peek(0))
                        push(peek(1))
                    }
                    read()
                    read()

                    if ((peek(0) == 'l'.toInt() || peek(0) == 'L'.toInt()) && peek(1) <= ' '.toInt()) {

                        if (lineNumber != 1 || columnNumber > 4)
                            error("PI must not start with xml")

                        parseStartTag(true)

                        if (attributeCount < 1 || "version" != attributes[2])
                            error("version expected")

                        version = attributes[3]

                        var pos = 1

                        if (pos < attributeCount && "inputEncoding" == attributes[2 + 4]) {
                            inputEncoding = attributes[3 + 4]!!
                            pos++
                        }

                        if (pos < attributeCount && "standalone" == attributes[4 * pos + 2]) {
                            val st = attributes[3 + 4 * pos]
                            when (st) {
                                "yes" -> standalone = true
                                "no" -> standalone = false
                                else -> error("illegal standalone value: $st")
                            }
                            pos++
                        }

                        if (pos != attributeCount)
                            error("illegal xmldecl")

                        isWhitespace = true
                        txtPos = 0

                        return XML_DECL
                    }
                }

                /*            int c0 = read ();
                        int c1 = read ();
                        int */

                term = '?'.toInt()
                result = XmlPullParser.PROCESSING_INSTRUCTION
            }
            '!'.toInt() -> when {
                peek(0) == '-'.toInt() -> {
                    result = XmlPullParser.COMMENT
                    req = "--"
                    term = '-'.toInt()
                }
                peek(0) == '['.toInt() -> {
                    result = XmlPullParser.CDSECT
                    req = "[CDATA["
                    term = ']'.toInt()
                    push = true
                }
                else -> {
                    result = XmlPullParser.DOCDECL
                    req = "DOCTYPE"
                    term = -1
                }
            }
            else -> {
                error("illegal: <$c")
                return XmlPullParser.COMMENT
            }
        }

        for (i in 0 until req.length)
            read(req[i])

        if (result == XmlPullParser.DOCDECL)
            parseDoctype(push)
        else {
            while (true) {
                c = read()
                if (c == -1) {
                    error(UNEXPECTED_EOF)
                    return XmlPullParser.COMMENT
                }

                if (push)
                    push(c)

                if ((term == '?'.toInt() || c == term)
                        && peek(0) == term
                        && peek(1) == '>'.toInt())
                    break

                prev = c
            }

            if (term == '-'.toInt() && prev == '-'.toInt() && !relaxed)
                error("illegal comment delimiter: --->")

            read()
            read()

            if (push && term != '?'.toInt())
                txtPos--

        }
        return result
    }

    /** precondition: &lt! consumed  */

    private fun parseDoctype(push: Boolean) {

        var nesting = 1
        var quoted = false

        // read();

        while (true) {
            val i = read()
            when (i) {

                -1 -> {
                    error(UNEXPECTED_EOF)
                    return
                }

                '\''.toInt() -> quoted = !quoted

                '<'.toInt() -> if (!quoted)
                    nesting++

                '>'.toInt() -> if (!quoted) {
                    if (--nesting == 0)
                        return
                }
            }
            if (push)
                push(i)
        }
    }

    /* precondition: &lt;/ consumed */

    private fun parseEndTag() {

        read() // '<'
        read() // '/'
        name = readName()
        skip()
        read('>')

        val sp = depth - 1 shl 2

        if (depth == 0) {
            error("element stack empty")
            eventType = XmlPullParser.COMMENT
            return
        }

        if (!relaxed) {
            if (name != elementStack[sp + 3]) {
                error("expected: /" + elementStack[sp + 3] + " read: " + name)

                // become case insensitive in relaxed mode

                //            int probe = sp;
                //            while (probe >= 0 && !name.toLowerCase().equals(elementStack[probe + 3].toLowerCase())) {
                //                stackMismatch++;
                //                probe -= 4;
                //            }
                //
                //            if (probe < 0) {
                //                stackMismatch = 0;
                //                //			text = "unexpected end tag ignored";
                //                eventType = COMMENT;
                //                return;
                //            }
            }

            namespace = elementStack[sp]!!
            prefix = elementStack[sp + 1]!!
            name = elementStack[sp + 2]!!
        }
    }

    private fun peekType(): Int {
        return when (peek(0)) {
            -1 -> XmlPullParser.END_DOCUMENT
            '&'.toInt() -> XmlPullParser.ENTITY_REF
            '<'.toInt() -> when (peek(1)) {
                '/'.toInt() -> XmlPullParser.END_TAG
                '?'.toInt(), '!'.toInt() -> LEGACY
                else -> XmlPullParser.START_TAG
            }
            else -> XmlPullParser.TEXT
        }
    }

    private operator fun get(pos: Int): String {
        return String(txtBuf, pos, txtPos - pos)
    }

    /*
    private final String pop (int pos) {
    String result = new String (txtBuf, pos, txtPos - pos);
    txtPos = pos;
    return result;
    }
    */

    private fun push(c: Int) {

        isWhitespace = isWhitespace and (c <= ' '.toInt())

        if (txtPos + 1 >= txtBuf.size) { // +1 to have enough space for 2 surrogates, if needed
            txtBuf = txtBuf.copyOf(txtPos * 4 / 3 + 4)
        }

        if (c > 0xffff) {
            // write high Unicode value as surrogate pair
            val offset = c - 0x010000
            txtBuf[txtPos++] = (offset.ushr(10) + 0xd800).toChar() // high surrogate
            txtBuf[txtPos++] = ((offset and 0x3ff) + 0xdc00).toChar() // low surrogate
        } else {
            txtBuf[txtPos++] = c.toChar()
        }
    }

    /** Sets name and attributes  */

    private fun parseStartTag(xmlDecimal: Boolean) {

        if (!xmlDecimal)
            read()
        name = readName()
        attributeCount = 0

        while (true) {
            skip()

            val c = peek(0)

            if (xmlDecimal) {
                if (c == '?'.toInt()) {
                    read()
                    read('>')
                    return
                }
            } else {
                if (c == '/'.toInt()) {
                    degenerated = true
                    read()
                    skip()
                    read('>')
                    break
                }

                if (c == '>'.toInt() && !xmlDecimal) {
                    read()
                    break
                }
            }

            if (c == -1) {
                error(UNEXPECTED_EOF)
                //eventType = COMMENT;
                return
            }

            val attrName = readName()

            if (attrName.isEmpty()) {
                error("attr name expected")
                //eventType = COMMENT;
                break
            }

            var i = attributeCount++ shl 2

            attributes = ensureCapacity(attributes, i + 4)

            attributes[i++] = ""
            attributes[i++] = null
            attributes[i++] = attrName

            skip()

            if (peek(0) != '='.toInt()) {
                if (!relaxed) {
                    error("Attr.value missing f. $attrName")
                }
                attributes[i] = attrName
            } else {
                read('=')
                skip()
                var delimiter = peek(0)

                if (delimiter != '\''.toInt() && delimiter != '"'.toInt()) {
                    if (!relaxed) {
                        error("attr value delimiter missing!")
                    }
                    delimiter = ' '.toInt()
                } else
                    read()

                val p = txtPos
                pushText(delimiter, true)

                attributes[i] = get(p)
                txtPos = p

                if (delimiter != ' '.toInt())
                    read() // skip endquote
            }
        }

        val sp = depth++ shl 2

        elementStack = ensureCapacity(elementStack, sp + 4)
        elementStack[sp + 3] = name

        if (depth >= nspCounts.size) {
            nspCounts = nspCounts.copyOf(depth + 4)
        }

        nspCounts[depth] = nspCounts[depth - 1]

        /*
        		if(!relaxed){
                for (int i = attributeCount - 1; i > 0; i--) {
                    for (int j = 0; j < i; j++) {
                        if (getAttributeName(i).equals(getAttributeName(j)))
                            exception("Duplicate Attribute: " + getAttributeName(i));
                    }
                }
        		}
        */
        if (processNsp)
            adjustNsp()
        else
            namespace = ""

        elementStack[sp] = namespace
        elementStack[sp + 1] = prefix
        elementStack[sp + 2] = name
    }

    /**
     * result: isWhitespace; if the setName parameter is set,
     * the name of the entity is stored in "name"  */

    private fun pushEntity() {

        push(read()) // &


        val pos = txtPos

        while (true) {
            val c = peek(0)
            if (c == ';'.toInt()) {
                read()
                break
            }
            if (c < 128
                    && (c < '0'.toInt() || c > '9'.toInt())
                    && (c < 'a'.toInt() || c > 'z'.toInt())
                    && (c < 'A'.toInt() || c > 'Z'.toInt())
                    && c != '_'.toInt()
                    && c != '-'.toInt()
                    && c != '#'.toInt()) {
                if (!relaxed) {
                    error("unterminated entity ref")
                }

                println("broken entitiy: " + get(pos - 1))

                //; ends with:"+(char)c);
                //                if (c != -1)
                //                    push(c);
                return
            }

            push(read())
        }

        val code = get(pos)
        txtPos = pos - 1
        if (token && eventType == XmlPullParser.ENTITY_REF) {
            name = code
        }

        if (code[0] == '#') {
            val c = if (code[1] == 'x')
                code.substring(2).toInt(16)
            else
                code.substring(1).toInt()
            push(c)
            return
        }

        val result = entityMap[code] as String

        unresolved = false

        if (unresolved) {
            if (!token)
                error("unresolved: &$code;")
        } else {
            for (i in 0 until result.length)
                push(result[i].toInt())
        }
    }

    /** types:
     * '<': parse to any token (for nextToken ())
     * '"': parse to quote
     * ' ': parse to whitespace or '>'
     */

    private fun pushText(delimiter: Int, resolveEntities: Boolean) {

        var next = peek(0)
        var cbrCount = 0

        while (next != -1 && next != delimiter) { // covers eof, '<', '"'

            if (delimiter == ' '.toInt())
                if (next <= ' '.toInt() || next == '>'.toInt())
                    break

            if (next == '&'.toInt()) {
                if (!resolveEntities)
                    break

                pushEntity()
            } else if (next == '\n'.toInt() && eventType == XmlPullParser.START_TAG) {
                read()
                push(' '.toInt())
            } else
                push(read())

            if (next == '>'.toInt() && cbrCount >= 2 && delimiter != ']'.toInt())
                error("Illegal: ]]>")

            if (next == ']'.toInt())
                cbrCount++
            else
                cbrCount = 0

            next = peek(0)
        }
    }

    private fun read(c: Char) {
        val a = read()
        if (a != c.toInt())
            error("expected: '" + c + "' actual: '" + a.toChar() + "'")
    }

    private fun read(): Int {
        val result: Int

        if (peekCount == 0)
            result = peek(0)
        else {
            result = peek[0]
            peek[0] = peek[1]
        }
        peekCount--

        columnNumber++

        if (result == '\n'.toInt()) {

            lineNumber++
            columnNumber = 1
        }

        return result
    }

    /** Does never read more than needed  */

    private fun peek(pos: Int): Int {

        while (pos >= peekCount) {

            val nw: Int

            when {
                srcBuf.size <= 1 -> nw = reader!!.next().toInt()
                srcPos < srcCount -> nw = srcBuf[srcPos++].toInt()
                else -> {
                    nw = if(!reader!!.hasNext()){
                        -1
                    }else{
                        reader!!.next().toInt()
                    }

                    srcPos = 1
                }
            }

            if (nw == '\r'.toInt()) {
                wasCR = true
                peek[peekCount++] = '\n'.toInt()
            } else {
                if (nw == '\n'.toInt()) {
                    if (!wasCR)
                        peek[peekCount++] = '\n'.toInt()
                } else
                    peek[peekCount++] = nw

                wasCR = false
            }
        }

        return peek[pos]
    }

    private fun readName(): String {

        val pos = txtPos
        var c = peek(0)
        if ((c < 'a'.toInt() || c > 'z'.toInt())
                && (c < 'A'.toInt() || c > 'Z'.toInt())
                && c != '_'.toInt()
                && c != ':'.toInt()
                && c < 0x0c0
                && !relaxed)
            error("name expected")

        do {
            push(read())
            c = peek(0)
        } while (c >= 'a'.toInt() && c <= 'z'.toInt()
                || c >= 'A'.toInt() && c <= 'Z'.toInt()
                || c >= '0'.toInt() && c <= '9'.toInt()
                || c == '_'.toInt()
                || c == '-'.toInt()
                || c == ':'.toInt()
                || c == '.'.toInt()
                || c >= 0x0b7)

        val result = get(pos)
        txtPos = pos
        return result
    }

    private fun skip() {

        while (true) {
            val c = peek(0)
            if (c > ' '.toInt() || c == -1)
                break
            read()
        }
    }

    override fun setInput(input: String) {
        this.reader = input.iterator()

        lineNumber = 1
        columnNumber = 0
        eventType = XmlPullParser.START_DOCUMENT
        degenerated = false
        attributeCount = -1
        version = null
        standalone = null

        if (reader == null)
            return

        srcPos = 0
        srcCount = 0
        peekCount = 0
        depth = 0

        entityMap = mutableMapOf(
            "amp" to "&",
            "apos" to "'",
            "gt" to ">",
            "lt" to "<",
            "quot" to "\""
        )
    }

    override fun getFeature(name: String): Boolean {
        return when {
            XmlPullParser.FEATURE_PROCESS_NAMESPACES == name -> processNsp
            isProp(name, false, "relaxed") -> relaxed
            else -> false
        }
    }

    override fun defineEntityReplacementText(entityName: String, replacementText: String) {
        if (entityMap.isEmpty())
            throw RuntimeException("Entity replacement text must be defined after setInput!")
        entityMap[entityName] = replacementText
    }

    override fun getProperty(name: String): Any? {
        if (isProp(name, true, "xmldecl-version"))
            return version
        if (isProp(name, true, "xmldecl-standalone"))
            return standalone
        return if (isProp(name, true, "location")) if (location != null) location else reader!!.toString() else null
    }

    override fun getNamespaceCount(depth: Int): Int {
        if (depth > this.depth)
            throw IndexOutOfBoundsException()
        return nspCounts[depth]
    }

    override fun getNamespacePrefix(pos: Int): String {
        return nspStack[pos shl 1]
    }

    override fun getNamespaceUri(pos: Int): String {
        return nspStack[(pos shl 1) + 1]
    }

    override fun getNamespace(prefix: String): String {

        if ("xml" == prefix)
            return "http://www.w3.org/XML/1998/namespace"
        if ("xmlns" == prefix)
            return "http://www.w3.org/2000/xmlns/"

        var i = (getNamespaceCount(depth) shl 1) - 2
        while (i >= 0) {
            if (prefix == nspStack[i])
                return nspStack[i + 1]
            i -= 2
        }
        throw Exception("No namespace")
    }

    override fun getTextCharacters(holderForStartAndLength: IntArray): CharArray? {
        if (eventType >= XmlPullParser.TEXT) {
            if (eventType == XmlPullParser.ENTITY_REF) {
                holderForStartAndLength[0] = 0
                holderForStartAndLength[1] = name.length
                val nameArray = CharArray(name.length)
                for (i in 0 until name.length) {
                    nameArray[i] = name[i]
                }
                return nameArray
            }
            holderForStartAndLength[0] = 0
            holderForStartAndLength[1] = txtPos
            return txtBuf
        }

        holderForStartAndLength[0] = -1
        holderForStartAndLength[1] = -1
        return null
    }

    override fun getAttributeType(index: Int): String {
        return "CDATA"
    }

    override fun isAttributeDefault(index: Int): Boolean {
        return false
    }

    override fun getAttributeNamespace(index: Int): String {
        if (index >= attributeCount)
            throw IndexOutOfBoundsException()
        return attributes[index shl 2]!!
    }

    override fun getAttributeName(index: Int): String {
        if (index >= attributeCount)
            throw IndexOutOfBoundsException()
        return attributes[(index shl 2) + 2]!!
    }

    override fun getAttributePrefix(index: Int): String {
        if (index >= attributeCount)
            throw IndexOutOfBoundsException()
        return attributes[(index shl 2) + 1]!!
    }

    override fun getAttributeValue(index: Int): String {
        if (index >= attributeCount)
            throw IndexOutOfBoundsException()
        return attributes[(index shl 2) + 3]!!
    }

    override fun getAttributeValue(namespace: String, name: String): String? {

        var i = (attributeCount shl 2) - 4
        while (i >= 0) {
            if (attributes[i + 2] == name && (attributes[i] == namespace))
                return attributes[i + 3]!!
            i -= 4
        }

        return null
    }

    override fun next(): Int {

        txtPos = 0
        isWhitespace = true
        var minType = 9999
        token = false

        do {
            nextImpl()
            if (eventType < minType)
                minType = eventType
            //	    if (curr <= TEXT) eventType = curr;
        } while (minType > XmlPullParser.ENTITY_REF // ignorable
                || minType >= XmlPullParser.TEXT && peekType() >= XmlPullParser.TEXT)

        eventType = minType
        if (eventType > XmlPullParser.TEXT)
            eventType = XmlPullParser.TEXT

        return eventType
    }

    override fun nextToken(): Int {

        isWhitespace = true
        txtPos = 0

        token = true
        nextImpl()
        return eventType
    }

    //
    // utility methods to make XML parsing easier ...

    override fun nextTag(): Int {

        next()
        if (eventType == XmlPullParser.TEXT && isWhitespace)
            next()

        if (eventType != XmlPullParser.END_TAG && eventType != XmlPullParser.START_TAG)
            exception("unexpected eventType")

        return eventType
    }

    override fun require(type: Int, namespace: String?, name: String?) {

        if (type != this.eventType
                || namespace != null && namespace != this.namespace
                || name != null && name != name)
            exception(
                    "expected: " + XmlPullParser.TYPES[type] + " {" + namespace + "}" + name)
    }

    override fun nextText(): String {
        if (eventType != XmlPullParser.START_TAG)
            exception("precondition: START_TAG")

        next()

        val result: String?

        if (eventType == XmlPullParser.TEXT) {
            result = text
            next()
        } else
            result = ""

        if (eventType != XmlPullParser.END_TAG)
            exception("END_TAG expected")

        return result!!
    }

    override fun setFeature(name: String, state: Boolean) {
        when {
            XmlPullParser.FEATURE_PROCESS_NAMESPACES == name -> processNsp = state
            isProp(name, false, "relaxed") -> relaxed = state
            else -> exception("unsupported feature: $name")
        }
    }

    override fun setProperty(name: String, value: Any) {
        if (isProp(name, true, "location"))
            location = value
        else
            throw XmlPullParserException("unsupported property: $name")
    }

    companion object {
        private const val UNEXPECTED_EOF = "Unexpected EOF"
        private const val ILLEGAL_TYPE = "Wrong event eventType"
        private const val LEGACY = 999
        private const val XML_DECL = 998
    }
}
