package com.example.pedulipasal.adapter.gamification

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.example.pedulipasal.data.model.gamification.QuestionsItem
import com.example.pedulipasal.databinding.ItemQuestionBinding
import com.example.pedulipasal.helper.Result
import com.google.ai.client.generativeai.type.GenerateContentResponse

class QuestionAdapter(
    private val context: Context,
    private val viewLifecycleOwner: LifecycleOwner,
    private val onItemSelectedCallback: OnItemSelected
) : RecyclerView.Adapter<QuestionAdapter.ViewHolder>() {

    private val questionList: MutableList<QuestionsItem?> = mutableListOf()

    interface OnItemSelected {
        fun onOptionClicked(chosenOption: String, correctAnswer: String, currPostion: Int)
        fun onAskGeminiClicked(prompt: String): LiveData<Result<GenerateContentResponse>>
    }

    class ViewHolder(val binding: ItemQuestionBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemQuestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val question = questionList[position] ?: QuestionsItem()
        var prompt = ""
        holder.binding.apply {
            tvQuestionItem.text = question.question ?: "No Question"
            prompt += "${question.question} \n"
            rgAnswerOptions.removeAllViews()

            question.options?.forEachIndexed { _, option ->
                val radioButton = RadioButton(context).apply {
                    text = option
                    id = View.generateViewId()
                }
                radioButton.setOnClickListener {
                    onItemSelectedCallback.onOptionClicked(
                        chosenOption = radioButton.text.toString(),
                        correctAnswer = question.correctAnswer ?: "No Answer",
                        currPostion = position
                    )

                    if (radioButton.text.toString() == question.correctAnswer) {
                        tvGeminiAnswer.visibility = View.GONE
                        for (i in 0 until rgAnswerOptions.childCount) {
                            val child = rgAnswerOptions.getChildAt(i)
                            if (child is RadioButton) {
                                child.isEnabled = false
                            }
                        }
                    }
                }
                prompt += "${radioButton.text} \n"
                rgAnswerOptions.addView(radioButton)
            }

            btnAskGemini.setOnClickListener {
                btnAskGemini.visibility = View.GONE
                val response = onItemSelectedCallback.onAskGeminiClicked(prompt)
                response.observe(viewLifecycleOwner) { result ->
                    if (result is Result.Success) {
                        progressBar.visibility = View.GONE
                        tvGeminiAnswer.visibility = View.VISIBLE
                        typeTextAnimation(tvGeminiAnswer, result.data.text ?: "No Answer", viewLifecycleOwner)
                    }
                    if (result is Result.Loading) {
                        progressBar.visibility = View.VISIBLE
                    }
                    if (result is Result.Error) {
                        progressBar.visibility = View.GONE
                        tvGeminiAnswer.text = result.error
                        tvGeminiAnswer.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    fun setQuestions(questions: List<QuestionsItem?>) {
        questionList.clear()
        questionList.addAll(questions)
        notifyDataSetChanged()
    }

    fun clearAllItems() {
        questionList.clear()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return questionList.size
    }

    private fun typeTextAnimation(
        textView: TextView,
        text: String,
        lifecycleOwner: LifecycleOwner,
        delay: Long = 75L
    ) {
        textView.text = ""
        var index = 0
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                if (index < text.length && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    textView.append(text[index].toString())
                    index++
                    handler.postDelayed(this, delay)
                }
            }
        }
        handler.post(runnable)

        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                handler.removeCallbacks(runnable)
            }
        })
    }

}
