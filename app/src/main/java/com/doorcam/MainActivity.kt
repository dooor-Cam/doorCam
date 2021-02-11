package branyo.homeCam


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


class MainActivity : AppCompatActivity() {
    private val esp8266UrlPath = "http://192.168.1.14"     //ESP8266的IP地址，192.168.1.14为通过路由器绑定静态IP
    private val mapNameIsToken = "token"
    private val tokenNum = 9527 //请求信息，需要与ESP8266接收信息对应
    private val uRL = "https://open.saintic.com/api/bingPic/"   //bing每日美图
    private var imgButton: ImageButton? = null
    private var txtView: TextView? = null
    private var backGroud: ImageView? = null
    private var lastClickTime: Long = 0 //按钮点击计时(防止连续点击)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
        setBackgroundView()
    }

    fun initView() {
        imgButton = findViewById<ImageButton>(R.id.imageButton)
        txtView = findViewById<TextView>(R.id.textView)
        backGroud = findViewById(R.id.globalBackground)
    }

    fun clickImageButton(view: View) {
        Toast.makeText(applicationContext, "请稍后……", Toast.LENGTH_LONG).show()
        if (isFastDoubleClick()) {
            Toast.makeText(applicationContext, "不可以连续点击", Toast.LENGTH_SHORT).show();
        } else
            clickButtonEvent(esp8266UrlPath, tokenNum)
    }

    private fun setBackgroundView() {   //设置背景的图片，调用bing的API
//        imgButton!!.setOnClickListener {
//            Toast.makeText(this, "请稍后……", Toast.LENGTH_LONG).show()
//            if (isFastDoubleClick()) {
//                Toast.makeText(this, "不可以连续点击", Toast.LENGTH_SHORT).show();
//            } else
//                clickButtonEvent(esp8266UrlPath, tokenNum)
//        }
        if (android.os.Build.VERSION.SDK_INT > 9) {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
        }
        var bitmap: Bitmap? = HttpUtil.okHTTPConnect(uRL)    //返回Bitmap类型对象
        Log.d("bitmap", bitmap.toString())
        try {
            if (bitmap == null)
                println("birmap为空")
            backGroud?.setImageBitmap(bitmap)   //设置背景图片
        } catch (e: Exception) {
        }
    }

    private fun clickButtonEvent(esp8266UrlPath: String, tokenNum: Int) {
        if (android.os.Build.VERSION.SDK_INT > 9) {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
        }
        if (ParseData.requestESP8266(esp8266UrlPath, mapNameIsToken, tokenNum)) {
            println("开锁成功!")
            Toast.makeText(this, "开锁成功", Toast.LENGTH_SHORT).show()
        } else {
            println("开锁失败!")
            Toast.makeText(this, "开锁失败！", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isFastDoubleClick(): Boolean {      //判断是否连续点击按钮
        val time = System.currentTimeMillis()
        if (time - lastClickTime < 1000) {   //距离上次点击小于1s
            return true
        }
        lastClickTime = time
        return false
    }
}


object ParseData {
    fun requestESP8266(esp8266UrlPath: String, str: String, tokenNum: Int): Boolean {
//    val map = mapOf("token" to 9527)    //创建需要传递的Json信息
//    println(map)
        val map = mapOf<String, Int>(str to tokenNum)
        println("生成的map是：$map")
        val responseJson: String = HttpUtil.httpPost(esp8266UrlPath, map)  //调用类方法httpPost
        Log.d("服务器返回的Json字符串", responseJson)
        val infoNum = parseJson(responseJson)
        if (infoNum == 7259) {
            println("验证返回数据成功")
            return true
        }
        return false
    }

    private fun parseJson(jsonData: String): Int {  //处理Json信息 返回的是7259
        try {
            val jsonObj = JSONObject(jsonData)
//            val jsonObject = jsonArray.getJSONObject(0)
            val id = jsonObj.getInt("info")
            println("info is $id")
            return id
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return -1
    }
}


object HttpUtil {
    fun okHTTPConnect(uRL: String): Bitmap? {   //loadPicture  Get方法
        var bitmap: Bitmap? = null
        try {
            var client: OkHttpClient = OkHttpClient()
            val request = Request.Builder()
                .url(uRL)
//                    .get()      //默认get请求可省略不写
                .build()
            var resPon = client.newCall(request)
                .execute()      //调用newCall方法创建一个Call对象，并调用execute方法发送请求并获取返回的数据
            try {
                if (resPon.isSuccessful) {   //成功响应
                    Log.d("loadPic", "成功响应,载入背景图片")
                    var inputStre = resPon.body?.byteStream()
                    bitmap = BitmapFactory.decodeStream(inputStre)
                }
            } catch (e: Exception) {
                println("读取数据流过程有问题")
            }
        } catch (e: Exception) {
            println("网络Get请求失败，检查网络连接")
        }
        return bitmap
    }

    fun httpPost(strUrlPath: String, params: Map<String, Int>): String { //Post请求
        val jsonStr = JSONObject(params)    //map转json
        println("要发送的Json为：$jsonStr")
//        val data = ("\r\n\r\n"+jsonStr.toString()).toByteArray()
        val data = jsonStr.toString().toByteArray()
        try {
            val url = URL(strUrlPath)
            val http = url.openConnection() as HttpURLConnection
            http.connectTimeout = 3000
            http.doInput = true
            http.doOutput = true
            http.requestMethod = "POST"
            http.useCaches = false//使用post方式不能用缓存
            //设置请求体的类型是文本类型
            http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
//            //设置请求体的长度
//            http.setRequestProperty("Content-Length", data.size.toString())

            //获得输出流，向服务器写入数据
            val out = http.outputStream
            out.write(data) //data为byte数组
            //获得输入流，接收服务器反馈回的信息
            val response = http.responseCode
            if (response == HttpURLConnection.HTTP_OK) {
                out.close()
                val inputStream = http.inputStream
                return dealResponseResult(inputStream)  //处理输入流信息，返回结果
            }
        } catch (ioe: IOException) {
            ioe.printStackTrace()
            return "错误:" + ioe.message.toString()
        }
        return "Program error"
    }


    private fun dealResponseResult(inputStream: InputStream): String {  //处理回复的输入流信息
        var resultData: String? = null      //存储处理结果
        val byteArrayOutputStream = ByteArrayOutputStream()
        val data = ByteArray(1024)
        var len = 0
        try {
            while (inputStream.read(data).apply { len = this } != -1) {
                byteArrayOutputStream.write(data, 0, len)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        resultData = String(byteArrayOutputStream.toByteArray())
        println("服务器反馈的信息处理完毕：$resultData")
        return resultData
    }
}