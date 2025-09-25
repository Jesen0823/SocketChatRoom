package org.dev.jesen.audiochatroomas

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity(), View.OnClickListener, AppContract.View {
    private var mInput: EditText? = null
    private var mSubmitButton: Button? = null
    private var mTipsView: TextView? = null
    private var mPresenter: AppContract.Presenter? = null
    private var mDialog: AlertDialog? = null
    private var mToast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 初始化提示
        mTipsView = findViewById<TextView>(R.id.txt_tips)
        mTipsView!!.text = Html.fromHtml(getResources().getString(R.string.tips))

        // 初始化点击按钮
        mSubmitButton = findViewById<Button>(R.id.btn_submit)
        mSubmitButton!!.setOnClickListener(this)

        // 初始化文字输入
        mInput = findViewById<EditText>(R.id.input)
        mInput!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                // 输入框变化时按钮跟随
                mSubmitButton!!.setText(if (s.isEmpty()) R.string.btn_random else R.string.btn_link)
            }
        })

        // 初始化Presenter
        mPresenter = Presenter(this)

        // 默认检查一次权限
        checkPermission()
    }

    override fun onClick(v: View?) {
        // 检查权限
        if (!checkPermission()) {
            Toast.makeText(this, R.string.toast_permission, Toast.LENGTH_SHORT).show()
            return
        }

        if (mInput!!.isEnabled()) {
            val code = mInput!!.getText().toString().trim { it <= ' ' }
            if (TextUtils.isEmpty(code)) {
                mPresenter!!.createRoom()
            } else {
                mPresenter!!.joinRoom(code)
            }
        } else {
            mPresenter!!.leaveRoom()
        }
    }

    override fun showProgressDialog(string: Int) {
        mDialog = AlertDialog.Builder(this)
            .setMessage(string)
            .setCancelable(false)
            .create()
        mDialog!!.show()
    }

    override fun dismissProgressDialog() {
        if (mDialog != null) {
            mDialog!!.dismiss()
            mDialog = null
        }
    }

    override fun showToast(string: Int) {
        if (mToast != null) {
            mToast!!.cancel()
        }
        mToast = Toast.makeText(this, string, Toast.LENGTH_LONG)
        mToast!!.show()
    }

    override fun showRoomCode(code: String?) {
        mInput!!.setText(code)
    }

    override fun onOnline() {
        mInput!!.setEnabled(false)
        mSubmitButton!!.setText(R.string.btn_unlink)
    }

    override fun onOffline() {
        mInput!!.setEnabled(true)
        mInput!!.setText("")
    }

    override fun onDestroy() {
        super.onDestroy()
        mPresenter!!.destroy()
    }

    /**
     * 检查权限
     */
    private fun checkPermission(): Boolean {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED)
            || (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                    != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET),
                MY_PERMISSIONS_REQUEST_RECORD_AUDIO
            )
            return false
        }
        return true
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // TODO 授权后的逻辑
            }
        }
    }

    companion object {
        private const val MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1
    }
}

