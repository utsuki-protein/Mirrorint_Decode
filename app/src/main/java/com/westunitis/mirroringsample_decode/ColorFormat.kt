package com.westunitis.mirroringsample_decode


class ColorFormat {

    fun getcolorformatString(cap:Int):String
    {
        val str: String

        when(cap){
            3 ->
                str = "COLOR_Format12bitRGB444"
            5 ->
                str = "COLOR_Format16bitARGB1555"
            4 ->
                str = "COLOR_Format16bitARGB4444"
            7 ->
                str = "COLOR_Format16bitBGR565"
            6 ->
                str = "COLOR_Format16bitRGB565"
            41 ->
                str = "COLOR_Format18BitBGR666"
            9 ->
                str = "COLOR_Format18bitARGB1665"
            8 ->
                str = "COLOR_Format18bitRGB666"
            10 ->
                str = "COLOR_Format19bitARGB1666"
            43 ->
                str = "COLOR_Format24BitABGR6666"
            42 ->
                str = "COLOR_Format24BitARGB6666"
            13 ->
                str = "COLOR_Format24bitARGB1887"
            12 ->
                str = "COLOR_Format24bitBGR888"
            11 ->
                str = "COLOR_Format24bitRGB888"
            14 ->
                str = "COLOR_Format25bitARGB1888"
            2130747392 ->
                str = "COLOR_Format32bitABGR8888"
            16 ->
                str = "COLOR_Format32bitARGB8888"
            15 ->
                str = "COLOR_Format32bitBGRA8888"
            2 ->
                str = "COLOR_Format8bitRGB332"
            27 ->
                str = "COLOR_FormatCbYCrY"
            28 ->
                str = "COLOR_FormatCrYCbY"
            36 ->
                str = "COLOR_FormatL16"
            33 ->
                str = "COLOR_FormatL2"
            37 ->
                str = "COLOR_FormatL24"
            38 ->
                str = "COLOR_FormatL32"
            34 ->
                str = "COLOR_FormatL4"
            35 ->
                str = "COLOR_FormatL8"
            1 ->
                str = "COLOR_FormatMonochrome"
            2134288520 ->
                str = "COLOR_FormatRGBAFlexible"
            2134292616 ->
                str = "COLOR_FormatRGBFlexible"
            31 ->
                str = "COLOR_FormatRawBayer10bit"
            30 ->
                str = "COLOR_FormatRawBayer8bit"
            32 ->
                str = "COLOR_FormatRawBayer8bitcompressed"
            2130708361 ->
                str = "COLOR_FormatSurface"
            25 ->
                str = "COLOR_FormatYCbYCr"
            26 ->
                str = "COLOR_FormatYCrYCb"
            18 ->
                str = "COLOR_FormatYUV411PackedPlanar"
            17 ->
                str = "COLOR_FormatYUV411Planar"
            2135033992 ->
                str = "COLOR_FormatYUV420Flexible"
            20 ->
                str = "COLOR_FormatYUV420PackedPlanar"
            39 ->
                str = "COLOR_FormatYUV420PackedSemiPlanar"
            19 ->
                str = "COLOR_FormatYUV420Planar"
            21 ->
                str = "COLOR_FormatYUV420SemiPlanar"
            2135042184 ->
                str = "COLOR_FormatYUV422Flexible"
            23 ->
                str = "COLOR_FormatYUV422PackedPlanar"
            40 ->
                str = "COLOR_FormatYUV422PackedSemiPlanar"
            22 ->
                str = "COLOR_FormatYUV422Planar"
            24 ->
                str = "COLOR_FormatYUV422SemiPlanar"
            2135181448 ->
                str = "COLOR_FormatYUV444Flexible"
            29 ->
                str = "COLOR_FormatYUV444Interleaved"
            2141391872 ->
                str = "COLOR_QCOM_FormatYUV420SemiPlanar"
            2130706688 ->
                str = "COLOR_TI_FormatYUV420PackedSemiPlanar"
            else ->
                str = "Not Regist : $cap"
        }

        return str
    }

}

