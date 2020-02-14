package com.westunitis.mirroringsample_decode

//import sun.jvm.hotspot.utilities.IntArray

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.westunitis.utilityinfolinker3.BluetoothChatService
import java.lang.Exception
import java.nio.ByteBuffer
import kotlin.math.floor


class MainActivity : AppCompatActivity() {
    private val REQUEST_CONNECT_DEVICE_SECURE = 1
    private val REQUEST_CONNECT_DEVICE_INSECURE = 2
    private val REQUEST_ENABLE_BT = 3
    private val REQUEST_MEDIA_PROJECTION = 4

    private val INITIALIZE = 0
    private val WAIT_INPUTBUFFER_AVAILABLE = 1
    private val HEADER_RECEIVED = 2
    private val FOOTER_RECEIVED = 3
    private val QUEUEING_INPUTBEFFER = 4

    val BODY = 0        // ボディのみ
    val HEAD = 1        // ヘッダのみ
    val HEAD_FOOT = 2   // ヘッダ+フッタ
    val FOOT_HEAD = 3   // フッタとヘッダ(2パケット混)
    val FOOT = 4        // フッタのみ


    // Message types sent from the BluetoothChatService Handler
    val MESSAGE_STATE_CHANGE = 1
    val MESSAGE_READ = 2
    val MESSAGE_WRITE = 3
    val MESSAGE_DEVICE_NAME = 4
    val MESSAGE_TOAST = 5

    // Key names received from the BluetoothChatService Handler
    val DEVICE_NAME = "device_name"
    val TOAST = "toast"

    private var mConnectedDeviceName: String? = null
    private var mConversationArrayAdapter: ArrayAdapter<String>? = null
    private var mOutStringBuffer: StringBuffer? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mChatService: BluetoothChatService? = null

    // MediaCodec
    // デコーダ
    lateinit var codec: MediaCodec
    lateinit var surface:Surface

    var isCodecBufAvailabile:Boolean = false
    var codecBufId: Int = -1

    lateinit var codecInputBuffer:ByteBuffer
    val queuingBuffer:ByteBuffer = ByteBuffer.allocate(1024 * 1024)
    val readBuf:ByteBuffer = ByteBuffer.allocate(1024 * 1024)

    var recvState = INITIALIZE

    // Surface→デコードイメージへの変換係数
    // Surface作成時に代入
    var scaleToDecodeX:Double = 0.0
    var scaleToDecodeY:Double = 0.0



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // バッファ初期化
        queuingBuffer.clear()
        readBuf.clear()

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            val activity = this
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show()
            activity.finish()
        }

    }

    override fun onStart() {
        super.onStart()
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter!!.isEnabled()) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat()
        }

    }

    override fun onResume() {
        super.onResume()

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            val state = mChatService?.stateSession
            if (state == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService?.start()
            }
        }

        // エンコードタイプ:H264に設定
        val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC

        // Surface作成完了後にcodecの設定、開始を行う
        val surfaceView = findViewById(R.id.surfaceView) as SurfaceView
        val surfaceHolder = surfaceView.holder
        surfaceHolder.addCallback(object: SurfaceHolder.Callback {
            override fun surfaceChanged(
                   holder: SurfaceHolder, format: Int,
                    width: Int, height: Int
            ){
                // デコーダが更新するときはSurafceChange呼ばれない
                println("surfaceChanged")
            }

            // surface Create後にcodecStartしないと落ちる
            override fun surfaceCreated(holder: SurfaceHolder) {
                println("surfaceCreated")
                //　Developperサイト推奨
                // https://developer.android.com/guide/topics/media/media-formats.html?authuser=2&hl=ja#video-encoding
                val decodeWidth:Double = 480.0
                val decodeHeight:Double = 360.0

                // 小数点３桁に丸める
                scaleToDecodeX = floor((decodeWidth / surfaceView.width) * 1000) / 1000
                scaleToDecodeY = floor((decodeHeight / surfaceView.height) * 1000) / 1000

                val bitRate = 5600000
                val fps = 30
                val format = createFomatInfo(
                    mimeType, decodeWidth.toInt(), decodeHeight.toInt(), bitRate, fps
                )

                // 確認用
                // getValueTypeForKeyメソッドはSDK.Q以上のみ
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                {
                    for (key in format.keys) {
                        val type = format.getValueTypeForKey(key)
                        println(key + "Type : " + type.toString())
                        when (type) {
                            1 -> println(key + "Value : " + format.getInteger(key))
                            4 -> println(key + "Value : " + format.getString(key))
                            else -> println("DoNothing")
                        }
                    }
                }

                // 出力先surfaceを取得
                surface = holder.surface

                // 第2引数のSurfaceがデコーダの出力先
                // 第4引数はエンコーダの時のみ指定
                codec.configure(format, surface, null, 0)
                codec.start()
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                println("surfaceDestroyed")
            }
        })


        try{
            //デコーダの取得
            codec = MediaCodec.createDecoderByType(mimeType)

            codec.setCallback(object : MediaCodec.Callback(){
                // 入力バッファ使用可能時
                override fun onInputBufferAvailable(mc:MediaCodec, inputBufferId:Int) {

                    Log.d("AASSAA_codec", "***onInputBufferAvailable***")
                    codecBufId = inputBufferId
                    codecInputBuffer = codec.getInputBuffer(inputBufferId)!!
                    isCodecBufAvailabile = true
                }
                // 出力バッファ使用可能時(フレーム作成完了時)
                override fun onOutputBufferAvailable(
                    mc: MediaCodec,
                    index: Int,
                    info: MediaCodec.BufferInfo
                ) {
                    println("***onOutputBufferAvailable***")
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        codec.releaseOutputBuffer(index, false)
                        return
                    }

                    // 第2引数がTrueならSurfaceに出力
                    codec.releaseOutputBuffer(index, true)
                }

                // フレームフォーマット変更時
                override fun onOutputFormatChanged(mc:MediaCodec, format:MediaFormat){
                    // Subsequent data will conform to new format.
                    // Can ignore if using getOutputFormat(outputBufferId)
                    println("onOutputFormatChanged : " + format.getString(MediaFormat.KEY_MIME)!!)
                }
                // Error時
                override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                    println(e.diagnosticInfo)
                }

            })
        }
        catch (e:Exception)
        {
            println("CreateCodecError")
        }

        // 端末のEnc/Decチェック
        // forDebug
        // dumpEncodeDecode()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CONNECT_DEVICE_SECURE -> {
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data!!, true)
                }
            }

        }
    }

    // TopMenuView
    // Bluetoothのアイコンメニュー作成
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        menuInflater.inflate(R.menu.bluetooth_icon, menu)
        return true
    }

    // 上部メニューのクリックイベント
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // whenの最後の式がReturnされる
        return when(item.itemId) {
            R.id.bluetoothIcon ->{
                // BluetoothFragmentの処理追加する
                val serverIntent = Intent(this, DeviceListActivity::class.java)
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE)
                true
            }
            else ->{
                super.onOptionsItemSelected(item)
            }
        }
    }

    // ミラーリング元にタッチイベントを発生させる
    // リモコン機能
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // Codecの初期化が終わるまでタッチイベント行わない
        if(event != null && scaleToDecodeX != 0.0) {
            if(event.getAction() == MotionEvent.ACTION_DOWN)
            {
                val x = event.x * scaleToDecodeX
                val y = event.y * scaleToDecodeY
                mHandler.obtainMessage(MESSAGE_WRITE, "TouchEvent:%.1f,%.1f".format(x, y)).sendToTarget()
            }
        }
        else
            println("MotionEvent is Null")

        return super.onTouchEvent(event)
    }

    /**
     * Establish connection with other device
     *
     * @param data   An [Intent] with [DeviceListActivity.EXTRA_DEVICE_ADDRESS] extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private fun connectDevice(data: Intent, secure: Boolean) {
        // Get the device MAC address
        val address = data.extras!!
            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS)
        // Get the BluetoothDevice object
        val device = mBluetoothAdapter!!.getRemoteDevice(address)
        // Attempt to connect to the device
        // なんやかんやでActivityResult使って接続先のMacAddressが帰ってくるので接続する
        mChatService?.connect(device, secure)
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private fun setupChat() {
        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = BluetoothChatService(this, mHandler)

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = StringBuffer("")
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private val mHandler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            val activity = this
            when (msg.what) {
                MESSAGE_STATE_CHANGE -> when (msg.arg1) {
                    BluetoothChatService.STATE_CONNECTED -> {
                        mConversationArrayAdapter?.clear()
                    }
                }
                MESSAGE_WRITE -> {
                    println(msg.obj)
                    mChatService?.write((msg.obj as String).toByteArray(Charsets.UTF_8))
                }

                MESSAGE_READ -> {
                    val msgArray = msg.obj as ByteArray
                    val msgSize = msg.arg1

                    // ヘッダフッタの検索
                    val pckComposition:Map<String, Int>
                            = analyzePacket(msgArray)

                    when(pckComposition["stat"])
                    {
                        // 取得漏れで古いフレーム受信中に新しいフレームのヘッダがきたら
                        // 古いフレームと混合しないようにバッファをクリアする
                        BODY -> readBuf.put(msgArray, 0, msgSize)
                        // ヘッダのみ含まれる場合
                        HEAD->
                        {
                            // 古いデータはクリア
                            readBuf.clear()
                            queuingBuffer.clear()
                            // ヘッダ位置からデータサイズ分コピー
                            readBuf.put(msgArray, pckComposition["head"]!!, msgSize)
                            recvState = HEADER_RECEIVED
                        }
                        // ヘッダとフッタが含まれる場合(同フレームのヘッダフッタ)
                        HEAD_FOOT ->
                        {
                            // ヘッダからフッタまでのサイズを取得
                            val length =  (pckComposition["end"]!!) - pckComposition["head"]!!
                            // 古いフレームはクリア
                            readBuf.clear()
                            queuingBuffer.clear()
                            // ヘッダ位置からデータサイズ分コピー
                            readBuf.put(msgArray, pckComposition["head"]!!, length)
                            // 1フレーム分をデコーダに渡すようのバッファにコピー
                            queuingBuffer.put(readBuf.array(), 0, readBuf.position())
                            recvState = WAIT_INPUTBUFFER_AVAILABLE
                        }
                        // ヘッダとフッタが含まれる場合(古いフレームの降ったと新しいフレームのヘッダ)
                        FOOT_HEAD ->
                        {
                            val length = pckComposition["end"]!!
                            // データサイズ分コピー
                            readBuf.put(msgArray, 0, length)
                            // 1フレーム分をデコーダに渡すようのバッファにコピー
                            queuingBuffer.put(readBuf.array(), 0, readBuf.position())
                            // デコーダのバッファにコピーしたので古いフレームはクリアしておく
                            readBuf.clear()
                            queuingBuffer.clear()
                            // 次のフレームのデータをコピー
                            readBuf.put(msgArray, length, msgSize - length)
                            recvState = WAIT_INPUTBUFFER_AVAILABLE
                        }
                        // フッタのみ
                        FOOT ->
                        {
                            val length = pckComposition["end"]!!
                            // データサイズ分コピー
                            readBuf.put(msgArray, 0, length)
                            // 1フレーム分をデコーダに渡すようのバッファにコピー
                            queuingBuffer.put(readBuf.array(), 0, readBuf.position())
                            // デコーダのバッファにコピーしたので古いフレームはクリアしておく
                            readBuf.clear()
                            recvState = WAIT_INPUTBUFFER_AVAILABLE
                        }
                    }

                    // 入力バッファ使用許可 + 入力フレーム格納済みの場合
                    if(isCodecBufAvailabile && recvState == WAIT_INPUTBUFFER_AVAILABLE) {
                        // フラグOff デコーダのCallbackによってOnされる
                        isCodecBufAvailabile = false

                        val bufDataSize = queuingBuffer.position()
                        codecInputBuffer.put(queuingBuffer.array(), 0, bufDataSize)
                        // 1フレーム分をデコーダのバッファにコピー
                        codec.queueInputBuffer(
                            codecBufId, 0,
                            bufDataSize, 0, 0
                        )

                        // デコーダのバッファにコピーしたので古いフレームはクリアしておく
                        codecInputBuffer.clear()
                        queuingBuffer.clear()
                        recvState = HEADER_RECEIVED
                    }
                }
                // Toastうるさいのでコメントアウト
                MESSAGE_DEVICE_NAME -> {
                    // save the connected device's name
                    mConnectedDeviceName = msg.data.getString(DEVICE_NAME)
                    if (null != activity) {
//                        Toast.makeText(activity, "Connected to ${mConnectedDeviceName}", Toast.LENGTH_SHORT).show()
                    }
                }
                // Toastうるさいのでコメントアウト
                MESSAGE_TOAST -> if (null != activity) {
//                    Toast.makeText(
//                        activity, msg.data.getString(TOAST),
//                        Toast.LENGTH_SHORT
//                    ).show()
                }
            }
        }
    }

    // forDebug
    fun dumpEncodeDecode() {
        val infoCnt = MediaCodecList.getCodecCount()
        val colorFormatChecker:ColorFormat = ColorFormat()
        repeat(infoCnt) {
            val codecInfo = MediaCodecList.getCodecInfoAt(it)

            print("Encoder Name : ")
            println(MediaCodecList.getCodecInfoAt(it).name)
            for(i in codecInfo.getSupportedTypes())
            {
                println(i)
                val type = codecInfo.getCapabilitiesForType(i)
                for(j in type.colorFormats)
                {
                    println(colorFormatChecker.getcolorformatString(j))
                }
            }
            println("**************----------------**************")
        }
        repeat(infoCnt) {
            if (MediaCodecList.getCodecInfoAt(it).isEncoder) {
                val codecInfo = MediaCodecList.getCodecInfoAt(it)
                println(
                    "Encoder: " +
                            codecInfo.supportedTypes[0] +
                            ", HardEncode:  " + codecInfo.isHardwareAccelerated.toString()
                )
            }
        }
        repeat(infoCnt) {
            if (!MediaCodecList.getCodecInfoAt(it).isEncoder) {
                val codecInfo = MediaCodecList.getCodecInfoAt(it)
                if(codecInfo.isHardwareAccelerated) {
                    println(
                        "Decoder: " +
                                codecInfo.name
                    )
                }
            }
        }
    }

    // デコーダ用Mediaformatの作成
    fun createFomatInfo(mimeType:String, width:Int, height:Int,
                        bitRate:Int, fps:Int):MediaFormat{
        val format = MediaFormat.createVideoFormat(mimeType, width, height)

        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
        )
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
        format.setInteger(MediaFormat.KEY_CAPTURE_RATE, fps)

        // この2つはエンコーダのみ必要
//        format.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
//        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)

        return format
    }

    // 戻り値map
    // head = ヘッダの位置
    // end = フッタの位置
    // stat = パケットの中身

    fun analyzePacket(msg:ByteArray):Map<String, Int>
    {
        // ヘッダとフッタの位置を格納する変数
        val map = mutableMapOf<String, Int>()

        // ヘッダはH264のstart_code_prefix_one_3bytesを基準にする
        // https://wikiwiki.jp/redstrange/H.264#s28fd525
        val header = byteArrayOf(0.toByte(), 0.toByte(), 0.toByte(), 1.toByte())

        findByteArray(header, msg)?.apply{
            map += "head" to this
        }

        // フッタは0x00000002->これはつかってもいい？いつかバグが出そう…
        // https://wikiwiki.jp/redstrange/H.264#s28fd525
        val footer = byteArrayOf(0.toByte(), 0.toByte(), 0.toByte(), 2.toByte())
        findByteArray(footer, msg)?.apply{
            map += "end" to this
        }

        // 2 or 3
        if("end" in map && "head" in map )
        {
            if(map["head"]!! < map["end"]!!) map += "stat" to HEAD_FOOT
            else map += "stat" to FOOT_HEAD
        }
        // 1
        else if("head" in map)
        {
            map += "stat" to HEAD
        }
        // 4
        else if("end" in map)
        {
            map += "stat" to FOOT
        }
        // 0
        else
        {
            map += "stat" to BODY
        }

        return map
    }

    // msgarrayの中にfindArrayが含まれるか検索する
    // 見つからなければNullを返す
    fun findByteArray(findArray:ByteArray, msgArray:ByteArray):Int? {
        msgArray.forEachIndexed { index, byte ->
            if(index+3 >= msgArray.size)
                return null
            if (byte == findArray[0]) {
                for (i in findArray.indices) {
                    if (msgArray[index + i] != findArray[i]) {
                        // return@forEachIndexedはContinueの意
                        return@forEachIndexed
                    }
                }
                return index
            }
        }
        return null
    }
}

