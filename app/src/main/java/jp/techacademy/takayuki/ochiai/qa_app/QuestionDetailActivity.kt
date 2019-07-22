package jp.techacademy.takayuki.ochiai.qa_app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.view.View
import android.widget.ListView

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*

import java.util.HashMap

class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {

        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras!!.get("question") as Question // テキストのままだとエラーになるため修正

        title = mQuestion.title

        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
            }
        }

        val dataBaseReference = FirebaseDatabase.getInstance().reference
        mAnswerRef = dataBaseReference
            .child(ContentsPATH)
            .child(mQuestion.genre.toString())
            .child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)

        // お気に入りボタンの表示・非表示
        displayClipFab(mQuestion)


    }

    private fun displayClipFab(mQuestion: Question) {
        val dataBaseReference = FirebaseDatabase.getInstance().reference
        var isCliped = false

        // 未ログイン時はお気に入りボタンを表示しない
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            clipFab.hide()
            return
        }

        // Firebaseからお気に入り一覧を取得し、お気に入り済みか判定してボタン画像を設定
        val clipRef = dataBaseReference
            .child(ClipPATH)
            .child(user.uid)
            .child(mQuestion.questionUid)
        clipRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                if (dataSnapshot.value == null) {
                    return
                }

                val map = dataSnapshot.value as Map<String, String>

                // TODO
                //  [0]->key:"status" value:"1", [1]->key:"genre" value:"1"のように配列形式でMapに格納されてしまい、
                //  "status"を直接指定できないため、for文で回して"status"を特定する
                for (item in map) {
                    if (item.key == "status") {
                        if (item.value == CLIP_ON.toString()) {
                            clipFab.setImageResource(R.drawable.clip_on_24dp)
                            isCliped = true
                        } else {
                            clipFab.setImageResource(R.drawable.clip_off_24dp)
                            isCliped = false
                        }
                    }
                }
            }

            override fun onCancelled(p0: DatabaseError) {
            }
        })

        // お気に入りボタンのリスナー設定
        clipFab.setOnClickListener {

            val data = HashMap<String, String>()

            data["genre"] = mQuestion.genre.toString() // clipのデータからcontentsのデータを取得するために必要

            if (isCliped) {
                // お気に入り解除
                data["status"] = CLIP_OFF.toString()
            } else {
                // お気に入り登録
                data["status"] = CLIP_ON.toString()
            }
            clipRef.setValue(data)
        }
    }
}
