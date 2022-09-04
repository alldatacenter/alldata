package com.alibaba.tesla.tkgone.server.services.config;

import com.alibaba.tesla.tkgone.server.domain.config.StopWord;
import com.alibaba.tesla.tkgone.server.domain.config.Synonym;
import com.alibaba.tesla.tkgone.server.domain.config.Word;
import com.alibaba.tesla.tkgone.server.domain.dao.StopWordMapper;
import com.alibaba.tesla.tkgone.server.domain.dao.SynonymMapper;
import com.alibaba.tesla.tkgone.server.domain.dao.WordMapper;
import com.alibaba.tesla.tkgone.server.domain.vo.SaveSynonymRequest;
import com.alibaba.tesla.tkgone.server.domain.vo.SaveWordsRequest;
import com.alibaba.tesla.tkgone.server.domain.vo.SaveWordsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * ES词典服务
 *
 * @author feiquan
 */
@Service
public class DictService {
    @Autowired
    private WordMapper wordMapper;
    @Autowired
    private StopWordMapper stopWordMapper;
    @Autowired
    private SynonymMapper synonymMapper;


    /**
     * 反馈
     * @param request
     * @return
     */
    public SaveWordsResponse saveWords(SaveWordsRequest request) {
        request.verify();

        SaveWordsResponse response = new SaveWordsResponse(request.getWords().length);
        for (String word : request.getWords()) {
            response.add(saveWord(word, request.getMemo()));
        }
        return response;
    }

    private Word saveWord(String word, String memo) {
        word = word.toLowerCase();
        Word record = wordMapper.selectByWord(word);
        if (record == null) {
            record = new Word();
            record.setGmtCreate(new Date());
            record.setGmtModified(record.getGmtCreate());
            record.setWord(word);
            record.setMemo(memo);
            wordMapper.insert(record);
        } else {
            record.setGmtModified(new Date());
            record.setMemo(memo);
            wordMapper.update(record);
        }
        return record;
    }

    private Word saveStopWord(String word, String memo) {
        word = word.toLowerCase();
        StopWord record = stopWordMapper.selectByWord(word);
        if (record == null) {
            record = new StopWord();
            record.setGmtCreate(new Date());
            record.setGmtModified(record.getGmtCreate());
            record.setWord(word);
            record.setMemo(memo);
            stopWordMapper.insert(record);
        } else {
            record.setGmtModified(new Date());
            record.setMemo(memo);
            stopWordMapper.update(record);
        }
        return record;
    }

    public List<Word> queryWords() {
        return wordMapper.selectAll();
    }

    public void deleteWord(int id) {
        wordMapper.delete(id);
    }

    public SaveWordsResponse saveStopWords(SaveWordsRequest request) {
        request.verify();

        SaveWordsResponse response = new SaveWordsResponse(request.getWords().length);
        for (String word : request.getWords()) {
            response.add(saveStopWord(word, request.getMemo()));
        }
        return response;
    }

    public void deleteStopWord(int id) {
        stopWordMapper.delete(id);
    }

    public List<StopWord> queryStopWords() {
        return stopWordMapper.selectAll();
    }

    public Synonym saveSynonym(SaveSynonymRequest request) {
        String word = request.getWord().toLowerCase();
        Synonym record = synonymMapper.selectByWord(word);
        if (record == null) {
            record = new Synonym();
            record.setGmtCreate(new Date());
            record.setGmtModified(record.getGmtCreate());
            record.setWord(word);
            record.setSynonyms(encode(request.getSynonyms()));
            record.setMemo(request.getMemo());
            synonymMapper.insert(record);
        } else {
            record.setGmtModified(new Date());
            record.setSynonyms(encode(request.getSynonyms()));
            record.setMemo(request.getMemo());
            synonymMapper.update(record);
        }
        return record;
    }

    private String encode(String[] synonyms) {
        StringBuilder sb = new StringBuilder();
        for (String item : synonyms) {
            item = item.trim().toLowerCase();
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(item);
        }
        return sb.toString();
    }

    public void deleteSynonym(int id) {
        synonymMapper.delete(id);
    }

    public List<Synonym> querySynonyms() {
        return synonymMapper.selectAll();
    }
}
