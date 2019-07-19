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

    val CLIP_OFF = 0
    val CLIP_ON = 1

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


    // クリップ一覧取得
    private fun getClipList(): ArrayList<String> {
        val clipList = ArrayList<String>()

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            // 空のリストを返却
            return ArrayList()
        }

        val dataBaseReference = FirebaseDatabase.getInstance().reference

        val clipRef = dataBaseReference
            .child(UsersPATH)
            .child(user.uid)
            .child(ClipPATH)

        clipRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val map = dataSnapshot.value as Map<String, Map<String, String>>
                for (item in map.values) {
                    if (item["status"] == CLIP_ON.toString()) {
                        clipList.add(item["questionUid"]!!)
                    }
                }
            }

            override fun onCancelled(p0: DatabaseError) {

            }
        })

        return clipList
    }


    private fun displayClipFab(mQuestion: Question) {
        // 未ログイン時はお気に入りボタンを表示しない
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            clipFab.hide()
            return
        }

        val a = getClipList()

        // お気に入り済みか判定し、ボタンの画像を設定
        if (getClipList().contains(mQuestion.questionUid)) {
            clipFab.setImageResource(R.drawable.clip_on_24dp)
        } else {
            clipFab.setImageResource(R.drawable.clip_off_24dp)
        }


        // お気に入りボタンのリスナー設定
        clipFab.setOnClickListener {
            // お気に入り済みならお気に入り解除
            // お気に入りしていなければお気に入り

            // TODO　とりあえずお気に入りできるようにしてみる

            val dataBaseReference = FirebaseDatabase.getInstance().reference
//            val clipRef = dataBaseReference
//                .child(ContentsPATH)
//                .child(mQuestion.genre.toString())
//                .child(mQuestion.questionUid)
//                .child(ClipPATH)
//            val data = HashMap<String, String>()
//            data["uid"] = user.uid
            val clipRef = dataBaseReference
                .child(UsersPATH)
                .child(user.uid)
                .child(ClipPATH)
//                .child(mQuestion.questionUid)
            val data = HashMap<String, String>()
            data["questionUid"] = mQuestion.questionUid
            data["status"] = CLIP_ON.toString()
            clipRef.push().setValue(data)
        }


    }
}
