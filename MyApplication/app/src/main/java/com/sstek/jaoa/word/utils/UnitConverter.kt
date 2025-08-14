package com.sstek.jaoa.word.utils

fun emuToPx(emu: Long): Int {
    return ((emu * 96) / 914400).toInt()
}

fun pxToEmu(px: Int): Int {
    return (px * 914400) / 96
}

