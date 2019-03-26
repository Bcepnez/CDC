package com.example.cdc_connect

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast

class MainActivity : AppCompatActivity() {

    lateinit var m_USBManager:UsbManager
    private var m_device:UsbDevice? = null
    private var m_serial:UsbSerialDevice? = null
    private var m_connection:UsbDeviceConnection? = null
    private val ACTION_USB_PERMISSION = "permission"
//    private val arr = byteArrayOfInts(0x02, 0x48, 0x32, 0x06, 0xD6, 0x00, 0x00, 0xBE, 0x06, 0x03)
    private val command = hexStringToByteArray("02483206D60000BE0603")
//    private val arr2 = HexData.hexToString(arr)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        m_USBManager = getSystemService(Context.USB_SERVICE) as UsbManager

        val  filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        registerReceiver(BroadcastReceiver, filter)

        con.setOnClickListener { Connect() }
        disCon.setOnClickListener { Disconect() }
        send.setOnClickListener { Send(textSend.text.toString()) }
        BackToMain.setOnClickListener { Send2(command) }
//        BackToMain.setOnClickListener { Send2(arr2.toByteArray()) }
    }
    fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)

        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }

        return data
    }
    fun byteArrayOfInts(vararg ints: Int) = ByteArray(ints.size) { pos -> ints[pos].toByte() }
    private fun Connect(){
        val usbDevices: HashMap<String,UsbDevice>? = m_USBManager.deviceList
        if(!usbDevices?.isEmpty()!!){
            var keep = true
            usbDevices.forEach { entry ->
                m_device = entry.value
                val deviceVendorId: Int? = m_device?.vendorId
                Log.i("Serial","Vendor Id : "+deviceVendorId)
                toast("Connected to Vendor Id : "+deviceVendorId)
//                03EB | 2307
                if(deviceVendorId == 0x03EB){
                    val intent: PendingIntent = PendingIntent.getBroadcast(this,0, Intent(ACTION_USB_PERMISSION),0)
                    m_USBManager.requestPermission(m_device,intent)
                    keep = false
                    Log.i("Serial","Connection Successful")
                    toast("Connected")
                } else {
                    m_connection = null
                    m_device = null
                    Log.i("Serial","Connection Fail")
                    toast("Fail Connected")
                }
                if (!keep){
                    return
                }
            }
        } else {
            Log.i("Serial","No USB Device connected")
            toast("No USB Device connected")
        }
    }
    private fun Disconect(){
        m_serial?.close()
    }
    private fun Send(textMessage:String){
//        m_serial?.write(textMessage.toByteArray())
        m_serial?.write(textMessage.toByteArray())
        Log.d("Send","Send data : "+textMessage.toByteArray())
        toast("Send data : "+textMessage)
        textSend.text.clear()
    }
    private fun Send2(ByteMessage:ByteArray){
//        m_serial?.write(textMessage.toByteArray())
        m_serial?.write(ByteMessage)
        Log.d("Send","Send data : "+ByteMessage)
        toast("Send data : "+ByteMessage+" : "+ByteMessage)
    }
    private val BroadcastReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            if(intent?.action!! == ACTION_USB_PERMISSION){
                val granted: Boolean = intent.extras!!.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)
                if(granted){
                    m_connection = m_USBManager.openDevice(m_device)
                    m_serial = UsbSerialDevice.createUsbSerialDevice(m_device,m_connection)
                    if (m_serial!=null){
                        if (m_serial!!.open()){
                            m_serial!!.setBaudRate(115200)
                            m_serial!!.setDataBits(UsbSerialInterface.DATA_BITS_8)
                            m_serial!!.setDataBits(UsbSerialInterface.STOP_BITS_1)
                            m_serial!!.setParity(UsbSerialInterface.PARITY_NONE)
                            m_serial!!.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
                        } else {
                            Log.d("Serial","Port not Open!!")
                        }
                    } else {
                        Log.d("Serial","Port == NULL!!")
                    }
                } else {
                    Log.d("Serial","Permission denied!!")
                }
            } else if (intent.action!! == UsbManager.ACTION_USB_DEVICE_ATTACHED){
                Connect()
            } else if (intent.action!! == UsbManager.ACTION_USB_DEVICE_DETACHED){
                Disconect()
            }
        }
    }
}
