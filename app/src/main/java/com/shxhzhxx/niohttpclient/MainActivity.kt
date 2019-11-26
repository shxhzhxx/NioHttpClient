package com.shxhzhxx.niohttpclient

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import java.net.InetSocketAddress
import java.net.URL
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.nio.charset.Charset
import java.util.concurrent.LinkedBlockingQueue


/**
 * TODO 实现keep-alive，复用socket
 * TODO 根据content-length读数据，而不是通过判断readNum<=0
 * TODO 支持HTTPS加密链接
 * TODO 支持常见header
 * */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val queue = LinkedBlockingQueue<Request>()
        val selector = Selector.open()
        Thread {
            while (true) {
                while (!queue.isEmpty()) {
                    val request = queue.poll() ?: continue
                    val channel = SocketChannel.open(request.address)
                    channel.configureBlocking(false)
                    channel.register(selector, SelectionKey.OP_READ, request)
                    channel.write(ByteBuffer.wrap(request.requestPacket))
                }
                while (selector.select() > 0) {
                    for (key in selector.selectedKeys()) {
                        if (!key.isReadable) continue
                        val request = key.attachment() as Request
                        val num = (key.channel() as SocketChannel).read(request.buffer)
                        if (num > 0) continue
                        key.channel().close()
                        key.cancel()
                        val response = String(
                            request.buffer.array(), 0, request.buffer.position(),
                            Charset.forName("utf-8")
                        )
                        val data = response.substringAfter("\r\n\r\n")
                        request.onLoad?.apply {
                            runOnUiThread { invoke(data) }
                        }
                    }
                    selector.selectedKeys().clear()
                }
            }
        }.start()

        fun performRequest() {
            queue.add(
                Request(
                    onLoad = { response ->
                        println("operate onLoad:\n$response")
                    },
                    method = "POST",
                    url = "http://api.1sapp.com/operate/resource/openScreen",
                    headers = listOf(
                        "User-Agent" to "qukan_android",
                        "Accept-Language" to "zh-CN,zh-CN;q=0.8,en-US;q=0.6"
                    ),
                    data = "OSVersion=8.0.0&brand=HONOR&client_version=30949000&device=866369036051342&deviceCode=866369036051342&device_code=866369036051342&distinct_id=dcaba0254364fa0d&dtu=004&guid=296cd595733325db155f5634173.92375903&is_pure=0&lat=0.0&lon=0.0&manufacturer=HUAWEI&model=BND-AL10&network=wifi&oaid=fa775dde-afff-0fb0-7ec7-d7fbffb7954a&time=1571975882587&tk=ACGnL6Aum3Uw5iFHC_tAJt40P-Kxluy7R700NzUxNDk1MDg5NTIyNQ&traceId=492cd7c78e2980f190edd3880c61ecfc&tuid=py-gLpt1MOYhRwv7QCbeNA&uuid=0c4b7d50be0c4e5891b2d0c3c9dbcb0d&version=30949000&versionName=3.9.49.000-debug&sign=3b685652d02f8f4f51bac902929e1aa3"
                )
            )
            queue.add(
                Request(
                    onLoad = { response ->
                        println("member onLoad:\n$response")
                    },
                    method = "POST",
                    url = "http://api.1sapp.com/member/isDeviceReport",
                    headers = listOf(
                        "User-Agent" to "qukan_android"
                    ),
                    data = "OSVersion=8.0.0&deviceCode=866369036051342&device_code=866369036051342&distinct_id=dcaba0254364fa0d&dtu=004&guid=296cd595733325db155f5634173.92375903&isNew=0&is_pure=0&lat=0.0&lon=0.0&network=wifi&oaid=fa775dde-afff-0fb0-7ec7-d7fbffb7954a&time=1571975882680&tk=ACGnL6Aum3Uw5iFHC_tAJt40P-Kxluy7R700NzUxNDk1MDg5NTIyNQ&traceId=492cd7c78e2980f190edd3880c61ecfc&tuid=py-gLpt1MOYhRwv7QCbeNA&uuid=0c4b7d50be0c4e5891b2d0c3c9dbcb0d&version=30949000&versionName=3.9.49.000-debug&sign=7afaa8c903127a3f55561241a733c9cc"
                )
            )
            queue.add(
                Request(
                    onLoad = { response ->
                        println("hotwords onLoad:\n$response")
                    },
                    headers = listOf(
                        "User-Agent" to "qukan_android"
                    ),
                    url = "http://api.1sapp.com/search/getHotwords?traceId=492cd7c78e2980f190edd3880c61ecfc&dtu=004&sign=19ff273309033c0984a75c208e28167b&lon=0.0&tuid=py-gLpt1MOYhRwv7QCbeNA&deviceCode=866369036051342&env=qukan_test&versionName=3.9.49.000-debug&currtime=1571975882595&uuid=0c4b7d50be0c4e5891b2d0c3c9dbcb0d&version=30949000&sign_key=bc4545e01f54de2d27fef33c0428d3a9&network=wifi&token=&distinct_id=dcaba0254364fa0d&is_pure=0&device_code=866369036051342&tk=ACGnL6Aum3Uw5iFHC_tAJt40P-Kxluy7R700NzUxNDk1MDg5NTIyNQ&OSVersion=8.0.0&guid=296cd595733325db155f5634173.92375903&from=feed&time=1571975882618&lat=0.0&oaid=fa775dde-afff-0fb0-7ec7-d7fbffb7954a"
                )
            )
            queue.add(
                Request(
                    onLoad = { response ->
                        println("baidu onLoad:\n$response")
                    },
                    url = "http://www.baidu.com"
                )
            )

            selector.wakeup()
        }

        go.setOnClickListener { performRequest() }
    }
}



class Request(
    private val method: String = "GET",
    url: String,
    headers: List<Pair<String, String>> = emptyList(),
    private val data: String = "",
    val onLoad: ((response: String) -> Unit)? = null
) {
    val buffer: ByteBuffer = ByteBuffer.allocate(100 * 1024)
    private val url = URL(url)
    private val headers = headers.plus(
        mutableListOf("Host" to this.url.host, "Connection" to "close").apply {
            if (data.isNotEmpty()) {
                add("Content-Type" to "application/x-www-form-urlencoded")
                add("Content-Length" to data.length.toString())
            }
        })
    val address get() = InetSocketAddress(url.host, url.defaultPort)
    val requestPacket: ByteArray
        get() {
            val path = url.file.let { if (it.isNullOrEmpty()) "/" else it }
            val headers = headers.joinToString(
                separator = "\r\n",
                postfix = "\r\n"
            ) { "${it.first}: ${it.second}" }
            return ("$method $path HTTP/1.1\r\n$headers\r\n$data").toByteArray()
        }
}