package jp.techacademy.takayuki.ochiai.qa_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.widget.ListView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.HashMap

class ClipActivity : AppCompatActivity() {

    private lateinit var mListView: ListView
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private lateinit var mAdapter: QuestionsListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clip)

        mListView = findViewById(R.id.listView)
        mAdapter = QuestionsListAdapter(this)
        mAdapter.notifyDataSetChanged()
        mQuestionArrayList = ArrayList<Question>()

        title = "お気に入り"
    }

    override fun onResume() {
        super.onResume()

        // リストビューのクリア
        mQuestionArrayList.clear()
        mAdapter.setQuestionArrayList(mQuestionArrayList)
        mListView.adapter = mAdapter

        // リストビューリスナー
        mListView.setOnItemClickListener { parent, view, position, id ->
            // Questionのインスタンスを渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position])
            startActivity(intent)
        }

        // clipから取得したお気に入りデータ一覧
        var clipIdList = HashMap<String, String>()

        val user = FirebaseAuth.getInstance().currentUser

        // Firebaseからお気に入りの質問IDの一覧を取得する
        val dataBaseReference = FirebaseDatabase.getInstance().reference
        val clipRef = dataBaseReference
            .child(ClipPATH)
            .child(user!!.uid)
        clipRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.value == null) {
                    return
                }
                val map = dataSnapshot.value as Map<String, Map<String, String>>
                for (item in map) {
                    if (item.value["status"] == CLIP_ON.toString()) {
                        clipIdList.put(item.key, item.value["genre"].toString())
                    }
                }

                // お気に入り質問IDに該当する、contentsのデータをFirebaseから取得する
                for (item in clipIdList) {
                    val genreRef = dataBaseReference
                        .child(ContentsPATH)
                        .child(item.value) // ジャンルID
                        .child(item.key) // 質問ID
                    genreRef.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {

                            if (dataSnapshot.value == null) {
                                return
                            }

                            val map = dataSnapshot.value as Map<String, String>
                            val title = map["title"] ?: ""
                            val body = map["body"] ?: ""
                            val name = map["name"] ?: ""
                            val uid = map["uid"] ?: ""
                            val imageString = map["image"] ?: ""
                            val bytes =
                                if (imageString.isNotEmpty()) {
                                    Base64.decode(imageString, Base64.DEFAULT)
                                } else {
                                    byteArrayOf()
                                }

                            val answerArrayList = ArrayList<Answer>()
                            val answerMap = map["answers"] as Map<String, String>?
                            if (answerMap != null) {
                                for (key in answerMap.keys) {
                                    val temp = answerMap[key] as Map<String, String>
                                    val answerBody = temp["body"] ?: ""
                                    val answerName = temp["name"] ?: ""
                                    val answerUid = temp["uid"] ?: ""
                                    val answer = Answer(answerBody, answerName, answerUid, key)
                                    answerArrayList.add(answer)
                                }
                            }

                            val question = Question(
                                title,
                                body,
                                name,
                                uid,
                                dataSnapshot.key ?: "",
                                item.value.toInt(),
                                bytes,
                                answerArrayList
                            )
                            mQuestionArrayList.add(question)

                            mAdapter.setQuestionArrayList(mQuestionArrayList)
                            mListView.adapter = mAdapter
                            mAdapter.notifyDataSetChanged()
                        }

                        override fun onCancelled(p0: DatabaseError) {

                        }
                    })
                }
            }

            override fun onCancelled(p0: DatabaseError) {
            }
        })
    }
}
