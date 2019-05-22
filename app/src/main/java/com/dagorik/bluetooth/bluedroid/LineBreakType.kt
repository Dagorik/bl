package com.dagorik.bluetooth.bluedroid

enum class LineBreakType constructor(val value: Int) {
    NONE(0),
    WINDOWS(1),
    UNIX(2),
    MAC(3),
    CRLF(1),
    LF(2),
    CR(3)
}
