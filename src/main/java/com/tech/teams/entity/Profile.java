package com.tech.teams.entity;

import com.yahoo.elide.annotation.Include;
import lombok.Data;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.ngram.NGramTokenizerFactory;
import org.hibernate.search.annotations.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.UUID;

@Data
@Entity
@Indexed
@Include(name = "profiles")
@AnalyzerDef(name = "case_insensitive",
        tokenizer = @TokenizerDef(factory = NGramTokenizerFactory.class, params = {
                @Parameter(name = "minGramSize", value = "3"),
                @Parameter(name = "maxGramSize", value = "10")
        }),
        filters = {
                @TokenFilterDef(factory = LowerCaseFilterFactory.class)
        }
)
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Fields({
            @Field(name = "name", index = Index.YES, store = Store.YES,
                    analyze = Analyze.YES, analyzer = @Analyzer(definition = "case_insensitive")),
            @Field(name = "sortName", analyze = Analyze.NO, store = Store.NO, index = Index.YES)
    })
    @SortableField(forField = "sortName")
    private String name;

    @Fields({
            @Field(name = "email", index = Index.YES, store = Store.YES,
                    analyze = Analyze.YES, analyzer = @Analyzer(definition = "case_insensitive"))
    })
    private String email;


    @Fields({
            @Field(name = "sortExperience", analyze = Analyze.NO, store = Store.NO, index = Index.YES)
    })
    @SortableField(forField = "sortExperience")
    private int experience;

    private String github;

    private String linkedIn;

    private String stackOverflow;
}
