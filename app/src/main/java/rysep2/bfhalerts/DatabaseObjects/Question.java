package rysep2.bfhalerts.DatabaseObjects;

import java.util.List;

/**
 * Created by Pascal on 08/04/2017.
 */

public class Question {

    private int id;
    private String question;
    private String answerTextDE;
    private boolean onlyForHelper;
    private List<String> answerList;
    private long questionType;

    public Question(){

    }

    public String getQuestion() {
        return question;
    }
    public String getAnswerTextDE() {
        return answerTextDE;
    }
    public boolean getOnlyForHelper() {
        return onlyForHelper;
    }
    public void setAnswerList(List<String> answerList){
        this.answerList = answerList;
    }
    public List<String> getAnswerList(){
        return answerList;
    }
    public long getQuestionType() {
        return questionType;
    }
}
