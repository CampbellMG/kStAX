/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// for license please see accompanying LICENSE.txt file (available also at http://www.xmlpull.org/)

package com.cmgcode.xml.xmlpull

import com.cmgcode.xml.xmlpull.XmlPullParser

/**
 * This exception is thrown to signal XML Pull Parser related faults.
 *
 * @author [Aleksander Slominski](http://www.extreme.indiana.edu/~aslom/)
 */
class XmlPullParserException : Exception {
    var detail: Throwable? = null
        protected set
    var lineNumber = -1
        protected set
    var columnNumber = -1
        protected set

    constructor(s: String) : super(s)

    constructor(msg: String?, parser: XmlPullParser?, chain: Throwable?) : super((if (msg == null) "" else "$msg ")
            + (if (parser == null) "" else "(position:" + parser.positionDescription + ") ")
            + if (chain == null) "" else "caused by: $chain") {

        if (parser != null) {
            this.lineNumber = parser.lineNumber
            this.columnNumber = parser.columnNumber
        }
        this.detail = chain
    }

}

