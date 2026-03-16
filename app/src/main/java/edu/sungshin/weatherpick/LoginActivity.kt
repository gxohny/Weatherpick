package edu.sungshin.weatherpick

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth

        val emailInput = findViewById<EditText>(R.id.emailEditText)
        val passwordInput = findViewById<EditText>(R.id.pwEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)      // 로그인 버튼
        val signUpTextView = findViewById<TextView>(R.id.signUpTextView) // "회원가입 하러가기" 같은 텍스트

        // 1. 로그인 버튼
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // 기본 입력 검증
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "올바른 이메일 형식을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(this, "비밀번호는 6자 이상 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔹 파이어베이스 로그인 시도 (로그인만!)
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
                        goToMain()
                    } else {
                        Toast.makeText(
                            this,
                            "로그인 실패. 이메일 또는 비밀번호를 확인해주세요.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        // 2. 회원가입 화면으로 이동
        signUpTextView.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    // 로그인 성공 후 메인 화면으로 이동
    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}
