package id.co.bca.pakar.be.doc.service.impl;

import id.co.bca.pakar.be.doc.client.ApiResponseWrapper;
import id.co.bca.pakar.be.doc.client.PakarOauthClient;
import id.co.bca.pakar.be.doc.dao.*;
import id.co.bca.pakar.be.doc.dto.*;
import id.co.bca.pakar.be.doc.exception.AccesDeniedDeleteContentException;
import id.co.bca.pakar.be.doc.exception.DataNotFoundException;
import id.co.bca.pakar.be.doc.exception.NotFoundArticleTemplateException;
import id.co.bca.pakar.be.doc.model.*;
import id.co.bca.pakar.be.doc.service.ArticleService;
import id.co.bca.pakar.be.doc.util.TreeArticleContents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;

import static id.co.bca.pakar.be.doc.common.Constant.Headers.BEARER;
import static id.co.bca.pakar.be.doc.common.Constant.Roles.ROLE_ADMIN;

@Service
public class ArticleServiceImpl implements ArticleService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${spring.article.param-tag:[]}")
    private String paramtTag;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ArticleTemplateRepository articleTemplateRepository;

    @Autowired
    private ArticleTemplateStructureRepository articleTemplateStructureRepository;

    @Autowired
    private ArticleTemplateContentRepository articleTemplateContentRepository;

    @Autowired
    private ArticleContentRepository articleContentRepository;

    @Autowired
    private StructureRepository structureRepository;

    @Autowired
    private SkReffRepository skReffRepository;

    @Autowired
    private ArticleImageRepository articleImageRepository;

    @Autowired
    private ArticleRefferenceRepository articleRefferenceRepository;

    @Autowired
    private PakarOauthClient pakarOauthClient;

    @Override
    @Transactional
    public Boolean existArticle(String title) {
        try {
            logger.info("verify existence of article title ---> {}", title);
            Boolean exist = articleRepository.existByArticleTitle(title);
            logger.info("title {} {}", title, exist.booleanValue() ? "exist in database" : "not exist in database");
            return exist;
        } catch (Exception e) {
            logger.error("exception", e);
            return Boolean.FALSE;
        }
    }

    /**
     * @param generateArticleDto
     * @return
     */
    @Override
    @Transactional(rollbackOn = {Exception.class})
    public ArticleDto generateArticle(GenerateArticleDto generateArticleDto) throws Exception {
        try {
            logger.info("generate article process");
            ArticleTemplateStructure articleTemplateStructure = articleTemplateStructureRepository.findArticleTemplates(generateArticleDto.getTemplateId(), generateArticleDto.getStructureId());
            ArticleTemplate template = null;
            if (articleTemplateStructure == null) {
                Optional<ArticleTemplate> templateOpt = articleTemplateRepository.findById(generateArticleDto.getTemplateId());
                if (templateOpt.isEmpty()) {
                    logger.info("data not found template");
                    throw new NotFoundArticleTemplateException("article template not found");
                }
                template = templateOpt.get();
            } else
                template = articleTemplateStructure.getArticleTemplate();

            logger.info("populate article");
            Article article = new Article();
            article.setCreatedBy(generateArticleDto.getUsername());
            article.setJudulArticle(generateArticleDto.getJudulArticle());
            article.setArticleTemplate(template.getId());
            article.setArticleUsedBy(generateArticleDto.getUsedBy());
            Structure structure = structureRepository.findStructure(generateArticleDto.getStructureId());
            article.setStructure(structure);

            logger.info("populate article contents");
            Iterable<ArticleTemplateContent> templateContents = articleTemplateContentRepository.findByTemplateId(template.getId());
            for (ArticleTemplateContent articleTemplateContent : templateContents) {
                ArticleContent articleContent = new ArticleContent();
                articleContent.setCreatedBy(generateArticleDto.getUsername());
                articleContent.setName(replaceTextByParams(articleTemplateContent.getName(), generateArticleDto.getParamKey(), generateArticleDto.getParamValue()));
                articleContent.setLevel(articleTemplateContent.getLevel());
                articleContent.setSort(articleTemplateContent.getSort());
                articleContent.setTopicCaption(articleTemplateContent.getTopicCaption());
                articleContent.setTopicContent(articleTemplateContent.getTopicContent());
                article.getArticleContents().add(articleContent);
                articleContent.setArticle(article);
            }

            logger.info("save article");
            article = articleRepository.save(article);
            logger.info("generate article success");

            // reset parent for article content
            for (ArticleContent articleContent : article.getArticleContents()) {
                for (ArticleTemplateContent articleTemplateContent : templateContents) {
                    if (articleContent.getName().equals(replaceTextByParams(articleTemplateContent.getName(), generateArticleDto.getParamKey(), generateArticleDto.getParamValue()))) {
                        if (articleTemplateContent.getParent() == null) {
                            articleContent.setParent(0L);
                            articleContentRepository.save(articleContent);
                            break;
                        } else {
                            Optional<ArticleTemplateContent> parent = articleTemplateContentRepository.findById(articleTemplateContent.getId());
                            if (!parent.isEmpty()) {
                                ArticleTemplateContent _parent = parent.get();
                                for (ArticleContent articleContent1 : article.getArticleContents()) {
                                    if (articleContent1.getName().equals(replaceTextByParams(_parent.getName(), generateArticleDto.getParamKey(), generateArticleDto.getParamValue()))) {
                                        articleContent.setParent(articleContent1.getId());
                                        articleContentRepository.save(articleContent);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            logger.info("populate response article");
            ArticleDto articleDto = new ArticleDto();
            articleDto.setId(article.getId());
            articleDto.setJudulArticle(article.getJudulArticle());
            articleDto.setShortDescription(article.getShortDescription());
            List<ArticleContentDto> articleContentDtos = new TreeArticleContents().menuTree(mapToListArticleContentDto(article.getArticleContents()));
            articleDto.setContents(articleContentDtos);

            return articleDto;
        } catch (NotFoundArticleTemplateException e) {
            logger.error("", e);
            throw new Exception("not found article template");
        } catch (Exception e) {
            logger.error("", e);
            throw new Exception("generate article failed");
        }
    }

    /**
     * @param id
     * @return
     * @throws Exception
     */
    @Override
    @Transactional
    public ArticleDto getArticleById(Long id) throws Exception {
        try {
            Optional<Article> articleOpt = articleRepository.findById(id);

            if (articleOpt.isEmpty()) {
                throw new DataNotFoundException("not found article with id --> " + id);
            }

            Article article = articleOpt.get();
            ArticleDto articleDto = new ArticleDto();
            articleDto.setId(article.getId());
            articleDto.setJudulArticle(article.getJudulArticle());
            articleDto.setShortDescription(article.getShortDescription());
            List<ArticleContentDto> articleContentDtos = new TreeArticleContents().menuTree(mapToListArticleContentDto(article.getArticleContents()));
            articleDto.setContents(articleContentDtos);
            Iterable<SkRefference> skRefferenceList = skReffRepository.findByArticleId(id);
            articleDto.setSkReff(mapToSkReffDto(skRefferenceList));
            Optional<Images> imageOpt = articleImageRepository.findByArticleId(article.getId());
            if (!imageOpt.isEmpty()) {
                Images image = imageOpt.get();
                articleDto.setImage(image.getUri());
            }
            Iterable<Article> realatedArticles = articleRefferenceRepository.findByArticleId(article.getId());
            articleDto.setRelated(mapToRelatedArticleDto(realatedArticles));
            return articleDto;
        } catch (Exception e) {
            logger.error("exception", e);
            throw new Exception("get article failed");
        }
    }

    /**
     * @param articleDto
     * @return
     */
    @Override
    @Transactional(rollbackOn = {Exception.class})
    public ArticleDto saveArticle(MultipartArticleDto articleDto) throws Exception {
        try {
            logger.info("save article process");
            Optional<Article> articleOpt = articleRepository.findById(articleDto.getId());
            if (articleOpt.isEmpty()) {
                logger.info("not found article data with id {}", articleDto.getId());
                throw new DataNotFoundException("data not found");
            }
            return null;
        } catch (Exception e) {
            logger.error("", e);
            throw new Exception("exception", e);
        }
    }

    /**
     * get content id
     *
     * @return
     * @throws Exception
     */
    @Override
    public Long getContentId() throws Exception {
        try {
            logger.info("get content id");
            Long contentId = articleContentRepository.getContentId();
            return contentId;
        } catch (Exception e) {
            logger.error("exception", e);
            throw new Exception("exception", e);
        }
    }

    /**
     * @param articleContentDto
     * @return
     * @throws Exception
     */
    @Override
    public ArticleContentDto saveContent(ArticleContentDto articleContentDto) throws Exception {
        try {
            logger.info("process save content");
            ArticleContent articleContent = new ArticleContent();
            articleContent.setCreatedBy(articleContentDto.getUsername());
            articleContent.setId(articleContentDto.getId());
            articleContent.setName(articleContentDto.getTitle());
            articleContent.setDescription(articleContentDto.getIntroduction());
            articleContent.setTopicCaption(articleContentDto.getTopicTitle());
            articleContent.setTopicContent(articleContentDto.getTopicContent());
            articleContent.setSort(articleContentDto.getOrder());
            articleContent.setLevel(articleContentDto.getLevel());
            Optional<ArticleContent> parentOpt = articleContentRepository.findById(articleContentDto.getParent());
            if (!parentOpt.isEmpty()) {
                articleContent.setParent(parentOpt.get().getId());
            }
            Optional<Article> articleOpt = articleRepository.findById(articleContentDto.getArticleId());
            if (!articleOpt.isEmpty()) {
                articleContent.setArticle(articleOpt.get());
            }
            logger.info("save article content to db");
            articleContent = articleContentRepository.save(articleContent);

            // reset list parent
            logger.info("get breadcumb article content");
            List<ArticleContent> parentArticleContents = articleContentRepository.findArticleContentParent(articleContent.getId());
            articleContentDto.setBreadcumbArticleContentDtos(mapToListParentArticleContentDto(parentArticleContents));
            return articleContentDto;
        } catch (Exception e) {
            logger.error("exception", e);
            throw new Exception("exception", e);
        }
    }

    /**
     * @param deleteContentDto
     * @return
     * @throws Exception
     */
    @Override
    public Boolean deleteContent(DeleteContentDto deleteContentDto) throws Exception {
        try {
            logger.info("delete content with id {}", deleteContentDto.getContentId());
            Optional<ArticleContent> articleContentOpt = articleContentRepository.findById(deleteContentDto.getContentId());
            if (articleContentOpt.isEmpty()) {
                logger.info("not found article content with id {}", deleteContentDto.getContentId());
                throw new DataNotFoundException("data not found");
            }
            logger.debug("call get roles api with token {}", deleteContentDto.getContentId());
            ResponseEntity<ApiResponseWrapper.RestResponse<List<String>>> restResponse = pakarOauthClient.getRoles(BEARER + deleteContentDto.getToken(), deleteContentDto.getUsername());
            logger.debug("response api request {}", restResponse);

            ArticleContent articleContent = articleContentOpt.get();
            List<String> roles = restResponse.getBody().getData();
            String role = roles.get(0);
            Long level = articleContent.getLevel();
            if (level.longValue() == 1) {
                logger.info("validate role user with level content");
                if (!role.equals(ROLE_ADMIN)) {
                    logger.info("user with role {} has no authorize to delete content level 1", role);
                    throw new AccesDeniedDeleteContentException("role " + role + " has no authorize delete content");
                }
            }

            logger.debug("username {} ---> has roles {}", deleteContentDto.getUsername(), roles);
            articleContent.setDeleted(Boolean.TRUE);
            articleContent.setModifyDate(new Date());
            articleContent.setModifyBy(deleteContentDto.getUsername());
            articleContentRepository.save(articleContent);
            List<ArticleContent> children = articleContentRepository.findArticleContent(articleContent.getId());
            // sorting root children
//            for(ArticleContent content : children) {
//            Collections.sort(children, new Comparator<ArticleContent>() {
//                @Override
//                public int compare(ArticleContent o1, ArticleContent o2) {
//                    return o1.getSort().intValue() - o2.getSort().intValue();
//                }
//            });
//            }
//            articleContent.getDeletedAllChildren(deleteContentDto.getUsername());
//            List<ArticleContent> children = articleContent.getAllChildren();
            for (ArticleContent content : children) {
                logger.debug("content level {} and title {}", content.getLevel(), content.getName());
                content.setModifyBy(deleteContentDto.getUsername());
                content.setModifyDate(new Date());
                content.setDeleted(Boolean.TRUE);
                articleContentRepository.save(content);
            }
            logger.info("delete article content set deleted value to true");
            return Boolean.TRUE;
        } catch (AccesDeniedDeleteContentException e) {
            logger.error("exception", e);
            throw new AccesDeniedDeleteContentException("has no authorize delete content level 1");
        } catch (Exception e) {
            logger.error("exception", e);
            throw new Exception("exception", e);
        }
    }

    /**
     *
     * @param searchDto
     * @return
     * @throws Exception
     */
    @Override
    public List<ArticleDto> search(SearchDto searchDto) throws Exception {
        return null;
    }

    private List<ArticleContentDto> mapToListArticleContentDto(Iterable<ArticleContent> iterable) {
        List<ArticleContentDto> listOfContents = new ArrayList<>();
        for (ArticleContent content : iterable) {
            ArticleContentDto contentDto = new ArticleContentDto();
            contentDto.setId(content.getId());
            contentDto.setLevel(content.getLevel());
            contentDto.setOrder(content.getSort());
            contentDto.setTitle(content.getName());
            if (content.getLevel().intValue() == 1)
                contentDto.setIntroduction(content.getDescription());
            contentDto.setParent(content.getParent());
            listOfContents.add(contentDto);
        }
        return listOfContents;
    }

    private List<SkReffDto> mapToSkReffDto(Iterable<SkRefference> iterable) {
        List<SkReffDto> listOfDtos = new ArrayList<>();
        for (SkRefference entity : iterable) {
            SkReffDto dto = new SkReffDto();
            dto.setId(entity.getId());
            dto.setTitle(entity.getTitle());
            dto.setSkNumber(entity.getSkNumber());
            listOfDtos.add(dto);
        }
        return listOfDtos;
    }

    private List<ArticleDto> mapToRelatedArticleDto(Iterable<Article> iterable) {
        List<ArticleDto> listOfDtos = new ArrayList<>();
        for (Article entity : iterable) {
            ArticleDto dto = new ArticleDto();
            dto.setId(entity.getId());
            dto.setVideoLink(entity.getVideLink());
            listOfDtos.add(dto);
        }
        return listOfDtos;
    }

    private List<BreadcumbArticleContentDto> mapToListParentArticleContentDto(Iterable<ArticleContent> iterable) {
        List<BreadcumbArticleContentDto> listOfContents = new ArrayList<>();
        for (ArticleContent content : iterable) {
            BreadcumbArticleContentDto contentDto = new BreadcumbArticleContentDto();
            contentDto.setId(content.getId());
            contentDto.setLevel(content.getLevel());
            contentDto.setName(content.getName());
            listOfContents.add(contentDto);
        }
        return listOfContents;
    }

    /**
     * get param from template
     *
     * @param text
     * @return
     */
    private List<String> getParams(String text) {
        logger.debug("split param tag {}", paramtTag);
        String[] tags = paramtTag.split("\\|");
        List<String> params = new ArrayList<>();
        for (int i = 0; i < tags.length; i++) {
            String tagEl = tags[i];
            Character openTag = tagEl.charAt(0);
            Character closeTag = tagEl.charAt(1);

            boolean startExtract = false;
            String param = "";
            for (int j = 0; j < text.length(); j++) {
                if (text.charAt(j) == closeTag) {
                    startExtract = false;
                    params.add(param.trim());
                }

                if (startExtract) {
                    param = param + text.charAt(j);
                }

                if (text.charAt(j) == openTag) {
                    startExtract = true;
                }
            }
        }
        return params;
    }

    /**
     * replace accordeon text with params
     *
     * @param replacedText
     * @param paramKey
     * @param paramValue
     * @return
     */
    private String replaceTextByParams(String replacedText, String paramKey, String paramValue) {
        logger.debug("split param tag {}", paramtTag);
        String[] tags = paramtTag.split("\\|");
        String key = paramKey;
        for (int i = 0; i < tags.length; i++) {
            String tagEl = tags[i];
            Character openTag = tagEl.charAt(0);
            Character closeTag = tagEl.charAt(1);
            paramKey = openTag + paramKey + closeTag;
            logger.debug("param key {} ---> param value {}", paramKey, paramValue);
            if (replacedText.contains(paramKey)) {
                replacedText = replacedText.replace(paramKey, paramValue);
                break;
            }
            paramKey = key;
        }
        logger.debug("replaced content title {}", replacedText);
        return replacedText;
    }
}
