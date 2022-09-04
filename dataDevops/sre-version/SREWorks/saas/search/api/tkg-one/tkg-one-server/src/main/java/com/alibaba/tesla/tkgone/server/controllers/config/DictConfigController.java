package com.alibaba.tesla.tkgone.server.controllers.config;

import com.alibaba.tesla.tkgone.server.common.DateUtil;
import com.alibaba.tesla.tkgone.server.common.http.RestResponse;
import com.alibaba.tesla.tkgone.server.domain.config.StopWord;
import com.alibaba.tesla.tkgone.server.domain.config.Synonym;
import com.alibaba.tesla.tkgone.server.domain.config.Word;
import com.alibaba.tesla.tkgone.server.domain.vo.SaveSynonymRequest;
import com.alibaba.tesla.tkgone.server.domain.vo.SaveWordsRequest;
import com.alibaba.tesla.tkgone.server.domain.vo.SaveWordsResponse;
import com.alibaba.tesla.tkgone.server.services.config.DictService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * ES词典管理
 *
 * @author feiquan
 */
@RestController
@RequestMapping("/config/dicts")
public class DictConfigController {
    @Autowired
    private DictService dictService;

    @RequestMapping(path = "/words/save", method = RequestMethod.POST)
    @ApiOperation(value = "", notes = "保存一批新的单词")
    public RestResponse<SaveWordsResponse> saveWords(
        @RequestBody @ApiParam(value = "请求参数") SaveWordsRequest request) {
        return new RestResponse<>(dictService.saveWords(request));
    }

    @RequestMapping(path = "/words/delete", method = RequestMethod.POST)
    @ApiOperation(value = "", notes = "删除指定的单词")
    public RestResponse<Void> deleteWord(
        @RequestParam @ApiParam(value = "请求参数") int id) {
        dictService.deleteWord(id);
        return new RestResponse<>();
    }

    @RequestMapping(path = "/words.txt", method = RequestMethod.GET)
    public ResponseEntity<Resource> getWords() throws IOException {
        List<Word> words = dictService.queryWords();
        return encodeStream(words);
    }

    @RequestMapping(path = "/stop-words/save", method = RequestMethod.POST)
    @ApiOperation(value = "", notes = "保存一批新的停用词")
    public RestResponse<SaveWordsResponse> saveStopWords(
        @RequestBody @ApiParam(value = "请求参数") SaveWordsRequest request) {
        return new RestResponse<>(dictService.saveStopWords(request));
    }

    @RequestMapping(path = "/stop-words/delete", method = RequestMethod.POST)
    @ApiOperation(value = "", notes = "删除指定的停用词")
    public RestResponse<Void> deleteStopWord(
        @RequestParam @ApiParam(value = "请求参数") int id) {
        dictService.deleteStopWord(id);
        return new RestResponse<>();
    }

    @RequestMapping(path = "/stop-words.txt", method = RequestMethod.GET)
    public ResponseEntity<Resource> getStopWords() throws IOException {
        List<StopWord> words = dictService.queryStopWords();
        return encodeStream(words);
    }

    @RequestMapping(path = "/synonyms/save", method = RequestMethod.POST)
    @ApiOperation(value = "", notes = "保存一个新的同义词")
    public RestResponse<Synonym> saveSynonym(
        @RequestBody @ApiParam(value = "请求参数") SaveSynonymRequest request) {
        return new RestResponse<>(dictService.saveSynonym(request));
    }

    @RequestMapping(path = "/synonyms/delete", method = RequestMethod.POST)
    @ApiOperation(value = "", notes = "删除指定的同义词")
    public RestResponse<Void> deleteSynonym(
        @RequestParam @ApiParam(value = "请求参数") int id) {
        dictService.deleteSynonym(id);
        return new RestResponse<>();
    }

    @RequestMapping(path = "/synonyms.txt", method = RequestMethod.GET)
    public ResponseEntity<Resource> getSynonyms() throws IOException {
        List<Synonym> synonyms = dictService.querySynonyms();
        return encodeStream(synonyms);
    }

    private ResponseEntity<Resource> encodeStream(List<? extends Word> words) {
        StringBuilder sb = new StringBuilder();
        Date lastModified = DateUtil.parse("2019-05-01", DateUtil.PATTERN_YYYYMMDD);
        for (Word word : words) {
            if (lastModified.before(word.getGmtModified())) {
                lastModified = word.getGmtModified();
            }
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(word.getWord());
            if (word instanceof Synonym) {
                sb.append(",").append(((Synonym)word).getSynonyms());
            }
        }
        return encodeStream(sb.toString(), lastModified);
    }

    private ResponseEntity<Resource> encodeStream(String text, Date modified) {
        ByteArrayResource resource = new ByteArrayResource(text.getBytes());
        return ResponseEntity.ok()
            .header("Last-Modified", DateUtil.format(modified, DateUtil.PATTERN_HTTP_HEADER))
            .contentLength(resource.contentLength())
            .contentType(MediaType.parseMediaType("application/octet-stream"))
            .body(resource);
    }
}
