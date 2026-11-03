package com.youai.crm.communication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youai.crm.knowledge.*;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.Pageable;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class RagIndexSynchronizerTest {
    KnowledgeRepository articles=mock(KnowledgeRepository.class);
    CustomerTranscriptSegmentRepository segments=mock(CustomerTranscriptSegmentRepository.class);
    RagSourceReader reader=mock(RagSourceReader.class);
    RagNotebookClient client=mock(RagNotebookClient.class);
    ObjectMapper json=new ObjectMapper();
    RagIndexSynchronizer worker=new RagIndexSynchronizer(articles,segments,reader,client);
    KnowledgeArticle article;
    @BeforeEach void setup()throws Exception{
        article=new KnowledgeArticle();article.id=11L;article.version=1L;article.title="公开资料";article.content="客户预算顾虑";
        when(articles.findById(11L)).thenReturn(Optional.of(article));
        when(articles.findByIdGreaterThanOrderByIdAsc(eq(0L),any(Pageable.class))).thenReturn(List.of(article));
        when(articles.findByIdGreaterThanOrderByIdAsc(eq(11L),any(Pageable.class))).thenReturn(List.of());
        when(segments.findByIdGreaterThanOrderByIdAsc(anyLong(),any(Pageable.class))).thenReturn(List.of());
        when(client.catalog()).thenReturn(json.createArrayNode());
    }
    @AfterEach void close(){worker.close();}
    @Test void uploadsOnlyChangedVersionsAndNoQueryIsRequired()throws Exception{
        worker.reconcile();
        verify(client).sync(eq("company"),eq("article:11"),eq(""),argThat(d->"客户预算顾虑".equals(d.get("content"))));
        assertEquals("READY",worker.status().state());
        clearInvocations(client);
        when(client.catalog()).thenReturn(json.valueToTree(List.of(Map.of("scope","company","id","article:11","version","1","token","token-1"))));
        worker.reconcile();
        verify(client,never()).sync(anyString(),anyString(),anyString(),any());
        verify(client,never()).search(anyString(),anyList());
        verify(client).cleanup();
    }
    @Test void removesDeletedSourcesEvenWhenUnrelatedEmbeddingFails()throws Exception{
        when(client.catalog()).thenReturn(json.valueToTree(List.of(Map.of("scope","company","id","article:99","version","1","token","old"))));
        when(articles.findById(99L)).thenReturn(Optional.empty());
        doThrow(new IllegalStateException("offline")).when(client).sync(eq("company"),eq("article:11"),anyString(),anyMap());
        worker.reconcile();
        verify(client).sync("company","article:99","old",null);
        assertEquals(1,worker.status().deleted());assertEquals("RETRY_PENDING",worker.status().state());
        doNothing().when(client).sync(eq("company"),eq("article:11"),anyString(),anyMap());
        worker.reconcile();
        assertEquals("READY",worker.status().state());
    }
    @Test void indexesTranscriptChangesUsingSameRendererAsQuestions()throws Exception{
        var segment=new CustomerTranscriptSegment();segment.id=21L;segment.customerNo="123";
        when(segments.findByIdGreaterThanOrderByIdAsc(eq(0L),any(Pageable.class))).thenReturn(List.of(segment));
        when(reader.render(segment)).thenReturn(new RagKnowledgeService.Material("segment:21","hash-v2","通话","销售确认的新跟进",null,21L,"123"));
        worker.reconcile();
        verify(client).sync(eq("customer:123"),eq("segment:21"),eq(""),argThat(d->"hash-v2".equals(d.get("version"))));
    }
}
