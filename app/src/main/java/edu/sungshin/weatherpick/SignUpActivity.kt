package edu.sungshin.weatherpick

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // 파이어베이스 초기화
        auth = Firebase.auth

        val emailInput = findViewById<EditText>(R.id.emailEditText)
        val passwordInput = findViewById<EditText>(R.id.pwEditText)
        val passwordCheckInput = findViewById<EditText>(R.id.pwCheckEditText) // 비밀번호 확인
        val signUpButton = findViewById<Button>(R.id.signUpButton)

        signUpButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val passwordCheck = passwordCheckInput.text.toString().trim()

            // 빈칸 체크
            if (email.isEmpty() || password.isEmpty() || passwordCheck.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "이메일 형식이 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "비밀번호는 6자 이상이어야 합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 두 비밀번호가 같은지 확인
            if (password != passwordCheck) {
                Toast.makeText(this, "비밀번호가 서로 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 파이어베이스 회원가입
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "회원가입 성공! 로그인 해주세요.", Toast.LENGTH_SHORT).show()
                        finish()   // 자동으로 LoginActivity로 돌아감
                    } else {
                        val errorMsg = when {
                            task.exception?.message?.contains("email-already") == true ->
                                "이미 가입된 이메일입니다."
                            task.exception?.message?.contains("invalid-email") == true ->
                                "이메일 형식이 잘못되었습니다."
                            else -> "회원가입 실패: ${task.exception?.message}"
                        }
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}
