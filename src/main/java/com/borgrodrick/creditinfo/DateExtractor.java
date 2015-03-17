package com.borgrodrick.creditinfo;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.AnnotationPipeline;
import edu.stanford.nlp.pipeline.PTBTokenizerAnnotator;
import edu.stanford.nlp.pipeline.WordsToSentencesAnnotator;
import edu.stanford.nlp.time.SUTime;
import edu.stanford.nlp.time.TimeAnnotations;
import edu.stanford.nlp.time.TimeAnnotator;
import edu.stanford.nlp.util.CoreMap;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Properties;

@Component()
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class DateExtractor {

    AnnotationPipeline pipeline;

    public DateExtractor(){
        pipeline = new AnnotationPipeline();
        pipeline.addAnnotator(new PTBTokenizerAnnotator(false));
        pipeline.addAnnotator(new WordsToSentencesAnnotator(false));

        //String modelDir = "C:\\creditinfo";

        //MaxentTagger tagger = new MaxentTagger(modelDir + "pos-tagger/english-bidirectional/english-bidirectional-distsim.tagger");
        //pipeline.addAnnotator(new POSTaggerAnnotator(tagger));

//        String sutimeRules = modelDir + "\\sutime\\defs.sutime.txt,"
//                + modelDir + "\\sutime\\english.holidays.sutime.txt,"
//                + modelDir + "\\sutime\\english.sutime.txt";
//        Properties props = new Properties();
//        props.setProperty("sutime.rules", sutimeRules);
//        props.setProperty("sutime.binders", "0");
        //pipeline.addAnnotator(new TimeAnnotator("sutime", props));

        pipeline.addAnnotator(new TimeAnnotator());
    }

    public String getDate(String input) {




        Annotation annotation = new Annotation(input);
        annotation.set(CoreAnnotations.DocDateAnnotation.class, SUTime.getCurrentTime().toString());
        pipeline.annotate(annotation);

        //System.out.println(annotation.get(CoreAnnotations.TextAnnotation.class));

        List<CoreMap> timexAnnsAll = annotation.get(TimeAnnotations.TimexAnnotations.class);
//        for (CoreMap cm : timexAnnsAll) {
//            List<CoreLabel> tokens = cm.get(CoreAnnotations.TokensAnnotation.class);
//            System.out.println(cm + " [from char offset " +
//                    tokens.get(0).get(CoreAnnotations.CharacterOffsetBeginAnnotation.class) +
//                    " to " + tokens.get(tokens.size() - 1).get(CoreAnnotations.CharacterOffsetEndAnnotation.class) + ']' +
//                    " --> " + cm.get(TimeExpression.Annotation.class).getTemporal());
//
//            System.out.println("--");
//        }

        if (!timexAnnsAll.get(0).toString().toLowerCase().contains("year")){
            return timexAnnsAll.get(0).toString();
        }

        else{
            return timexAnnsAll.get(1).toString();
        }



    }
}
