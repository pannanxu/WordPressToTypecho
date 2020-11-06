package io.mvvm;

import com.overzealous.remark.Remark;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class Application {

    // WordPress 应用程序后台备份后得到的xml文件路径
    private static final String WORDPRESS_BACK_XML_PATH = "E:\\WordPressToTypecho\\src\\main\\resources\\demo.xml";

    private static StringBuilder sql = new StringBuilder();

    // 标签/分类的ID和名称
    private static List<Map<String, String>> tags = new ArrayList<>();

    // 文章的ID和标签/分类，列表
    private static List<Map<String, String>> posts = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        SAXReader reader = new SAXReader();
        Document document = reader.read(new File(WORDPRESS_BACK_XML_PATH));
        Element node = document.getRootElement();
        Element element = node.element("channel");
        List<Element> elements = element.elements();
        elements.forEach(e -> {
            if (e.getName().equals("category")) {
                builderCategory(e);
            }
            if (e.getName().equals("tag")) {
                builderTag(e);
            }
            if (e.getName().equals("item")) {
                builderItem(e);
            }
        });
        builderRela();
        System.out.println(sql.toString());
    }

    // 分类
    private static void builderCategory(Element element) {
        // 分类ID
        String termId = element.element("term_id").getText();
        // 分类别名
        String categoryNiceName = element.element("category_nicename").getText();
        // 上级ID
        String categoryParent = element.element("category_parent").getText();
        // 分类名称
        String catName = element.element("cat_name").getText();

        Map<String, String> map = new HashMap<>();
        map.put("id", termId);
        map.put("name", catName);
        tags.add(map);

        sql.append("insert into typecho_metas (mid, name, slug, type, parent) values(")
                .append(termId).append(",")
                .append("'").append(catName).append("',")
                .append("'").append(categoryNiceName).append("',")
                .append("'category',")
                .append("".equals(categoryParent) ? 0 : categoryParent)
                .append(");");
    }

    // 标签
    private static void builderTag(Element element) {
        // 标签ID
        String termId = element.element("term_id").getText();
        // 标签链接
        String termSlug = element.element("tag_slug").getText();
        // 标签名称
        String termName = element.element("tag_name").getText();

        Map<String, String> map = new HashMap<>();
        map.put("id", termId);
        map.put("name", termName);
        tags.add(map);

        sql.append("insert into typecho_metas (mid, name, slug, type) values(")
                .append(termId).append(",")
                .append("'").append(termName).append("',")
                .append("'").append(termSlug).append("',")
                .append("'tag'")
                .append(");");
    }

    // 文章或页面
    private static void builderItem(Element element) {
        // 文章标题
        String title = element.element("title").getText();
        // 文章链接
        String link = element.element("link").getText();
        // 发布时间
        String postDate = element.element("post_date").getText();
        String postDateLong = date2TimeStamp(postDate, "yyyy-MM-dd HH:mm:ss");
        // 更新时间
        String postDateGmt = element.element("post_date_gmt").getText();
        String postDateGmtLong = date2TimeStamp(postDateGmt, "yyyy-MM-dd HH:mm:ss");
        // 发布用户
        String creator = element.element("creator").getText();
        // 文章摘要
        String descripttion = element.element("description").getText();
        // 文章内容
        String encoded = element.element("encoded").getText().replaceAll("<!--.*-->", "").replaceAll("\n", "");
        String content = "<!--markdown-->\n" + htmlToMd(encoded).replace("'", "\\'");
        // 文章ID
        String postId = element.element("post_id").getText();
        // 文章url
        String postName = element.element("post_name").getText();
        // 文章状态，发布，审核，草稿。
        // tp：公布：publish，隐藏：hidden，待审核：waiting
        String status = element.element("status").getText();
        // 文章类型：page表示页面，post表示文章
        String postType = element.element("post_type").getText();

        sql.append("insert into typecho_contents (`cid`, `title`, `slug`, `created`, `modified`, `text`, `authorId`, `type`, `status`) values(")
                .append("'").append(postId).append("',")
                .append("'").append(title).append("',")
                .append("'").append(postName).append("',")
                .append(postDateLong).append(",")
                .append(postDateGmtLong).append(",")
                .append("'").append(content).append("',")
                .append("'1',")
                .append("'").append(postType).append("',")
                .append("'").append("publish".equals(status) ? "publish" : "waiting").append("'")
                .append(");");

        builderPostTags(element, postId);
        builderComments(element, postId);
    }

    // 文章标签
    private static void builderPostTags(Element element, String postId) {
        List<Element> category = element.elements("category");
        category.forEach(e -> {
            String text = e.getText();
            Map<String, String> map = new HashMap<>();
            map.put("name", text);
            map.put("id", postId);
            posts.add(map);
        });
    }

    // 文章评论
    private static void builderComments(Element element, String postId) {
        List<Element> comments = element.elements("comment");
        if (null != comments && !comments.isEmpty()) {
            comments.forEach(e -> {
                // 评论ID
                String commentId = e.element("comment_id").getText();
                // 评论人名称
                String commentAuthor = e.element("comment_author").getText();
                // 评论人邮箱
                String commentAuthorEmail = e.element("comment_author_email").getText();
                // 评论人主页URL
                String commentAuthorUrl = e.element("comment_author_url").getText();
                // 评论人IP
                String commentAuthorIp = e.element("comment_author_IP").getText();
                // 评论时间
                String commentDate = e.element("comment_date").getText();
                String commentDateLong = date2TimeStamp(commentDate, "yyyy-MM-dd HH:mm:ss");
                // 评论内容
                String commentContent = e.element("comment_content").getText();
                // 1表示 评论以批准
                String commentApproved = e.element("comment_approved").getText();
                // 评论上级ID
                String commentParent = e.element("comment_parent").getText();
                // 评论用户ID
                String commentUserId = e.element("comment_user_id").getText();

                sql.append("insert into typecho_comments (`coid`, `cid`, `author`, `authorId`, `mail`, `url`, `ip`, `text`, `parent`, `created`) values (")
                        .append(commentId).append(",")
                        .append(postId).append(",")
                        .append("'").append(commentAuthor).append("',")
                        .append("'").append(commentUserId).append("',")
                        .append("'").append(commentAuthorEmail).append("',")
                        .append("'").append(commentAuthorUrl).append("',")
                        .append("'").append(commentAuthorIp).append("',")
                        .append("'").append(commentContent).append("',")
                        .append("'").append("".equals(commentParent) ? 0 : commentParent).append("',")
                        .append(commentDateLong)
                        .append(");");
            });
        }
    }

    // 构建文章和分类以及标签的关联表
    private static void builderRela() {
        posts.forEach(post -> {
            String postName = post.get("name");
            String postId = post.get("id");
            tags.stream().filter(tag -> tag.get("name").equals(postName)).forEach(e -> {
                sql.append("insert into typecho_relationships values (")
                        .append(postId).append(",")
                        .append(e.get("id"))
                        .append(");");
            });
        });
    }

    // 获取时间戳
    private static String date2TimeStamp(String date_str, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            return String.valueOf(sdf.parse(date_str).getTime() / 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    // html转MarkDown
    private static String htmlToMd(String html) {
        return new Remark().convertFragment(html);
    }
}
