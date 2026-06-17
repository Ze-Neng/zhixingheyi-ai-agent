package com.zeneng.zhixingheyi.service.impl;

import com.zeneng.zhixingheyi.service.MarkdownParserService;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Block;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class MarkdownParserServiceImpl implements MarkdownParserService {

    private final Parser parser;
    private String originalMarkdownContent;

    @Value("${document.chunk.size:800}")
    private int chunkSize;

    @Value("${document.chunk.overlap:100}")
    private int chunkOverlap;

    public MarkdownParserServiceImpl() {
        MutableDataSet options = new MutableDataSet();
        this.parser = Parser.builder(options).build();
    }

    @Override
    public List<MarkdownSection> parseMarkdown(InputStream inputStream) {
        try {
            originalMarkdownContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            Document document = parser.parse(originalMarkdownContent);

            // 第一步：按标题解析为 sections
            List<MarkdownSection> sections = extractSections(document);

            // 第二步：处理无标题文档的情况
            if (sections.isEmpty() && originalMarkdownContent != null && !originalMarkdownContent.trim().isEmpty()) {
                // 整个文档作为一个 section，title 为空
                sections.add(new MarkdownSection("", originalMarkdownContent.trim(), 0));
            }

            // 第三步：对超过 chunkSize 的 section 进行固定大小切分
            List<MarkdownSection> result = new ArrayList<>();
            for (MarkdownSection section : sections) {
                List<MarkdownSection> chunked = splitSection(section);
                result.addAll(chunked);
            }

            log.info("解析 Markdown 完成，共提取 {} 个章节，切分为 {} 个 chunks (chunkSize={}, overlap={})",
                    sections.size(), result.size(), chunkSize, chunkOverlap);
            return result;

        } catch (Exception e) {
            log.error("解析 Markdown 失败", e);
            throw new RuntimeException("解析 Markdown 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 对一个 section 进行固定大小切分，标题边界处不强行切断
     */
    private List<MarkdownSection> splitSection(MarkdownSection section) {
        String content = section.getContent();
        String title = section.getTitle();
        int level = section.getLevel();

        if (content == null || content.length() <= chunkSize) {
            return Collections.singletonList(section);
        }

        List<MarkdownSection> result = new ArrayList<>();
        int start = 0;
        int safetyCounter = 0;

        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());

            // 如果不是最后一个 chunk，尝试在合适的断点处切断
            if (end < content.length()) {
                end = findBreakPoint(content, start, end);
            }

            String chunkText = content.substring(start, end).trim();
            if (!chunkText.isEmpty()) {
                result.add(new MarkdownSection(title, chunkText, level));
            }

            // 滑动窗口：下次起点 = end - overlap
            start = end - chunkOverlap;
            if (start >= content.length()) break;
            if (start < 0) start = 0;

            safetyCounter++;
            if (safetyCounter > 1000) {
                log.warn("splitSection 超过最大迭代次数，强制退出: title={}", title);
                break;
            }
        }

        return result;
    }

    /**
     * 在 chunk 范围内寻找最佳断点（段落边界、句子边界等）
     * 优先在 chunkSize 附近找最近的段落换行符，其次找句子结束符
     */
    private int findBreakPoint(String content, int start, int end) {
        String searchWindow = content.substring(start, end);

        // 优先在段落边界（双换行）处切断
        int paraBreak = searchWindow.lastIndexOf("\n\n");
        if (paraBreak > chunkSize * 0.5) {
            return start + paraBreak + 2;
        }

        // 其次在单换行处切断
        int lineBreak = searchWindow.lastIndexOf("\n");
        if (lineBreak > chunkSize * 0.5) {
            return start + lineBreak + 1;
        }

        // 否则在句子结束符处切断
        for (String punct : new String[]{". ", "。", "! ", "！", "? ", "？"}) {
            int idx = searchWindow.lastIndexOf(punct);
            if (idx > chunkSize * 0.5) {
                return start + idx + punct.length();
            }
        }

        // 都没有则直接在 chunkSize 处切断
        return end;
    }

    /**
     * 提取标题和内容
     * 只遍历文档的直接子节点，遇到任何标题就停止收集当前标题的内容
     */
    private List<MarkdownSection> extractSections(Document document) {
        // 收集文档的所有直接子节点（顶层节点）
        List<Node> topLevelNodes = new ArrayList<>();
        Node child = document.getFirstChild();
        while (child != null) {
            topLevelNodes.add(child);
            child = child.getNext();
        }

        List<MarkdownSection> sections = new ArrayList<>();

        // 遍历顶层节点，找到所有标题
        for (int i = 0; i < topLevelNodes.size(); i++) {
            Node node = topLevelNodes.get(i);

            if (node instanceof Heading) {
                Heading heading = (Heading) node;
                String title = extractHeadingText(heading);

                if (title == null || title.trim().isEmpty()) {
                    continue;
                }

                // 收集当前标题到下一个标题（任何级别）之间的所有内容
                StringBuilder contentBuilder = new StringBuilder();
                for (int j = i + 1; j < topLevelNodes.size(); j++) {
                    Node nextNode = topLevelNodes.get(j);

                    // 如果遇到任何标题，停止收集
                    if (nextNode instanceof Heading) {
                        break;
                    }

                    // 提取节点内容
                    String content = extractNodeContent(nextNode);
                    if (content != null && !content.trim().isEmpty()) {
                        if (contentBuilder.length() > 0) {
                            contentBuilder.append("\n");
                        }
                        contentBuilder.append(content);
                    }
                }

                String content = contentBuilder.toString().trim();
                sections.add(new MarkdownSection(title, content, heading.getLevel()));
            }
        }

        return sections;
    }

    /**
     * 提取标题文本
     */
    private String extractHeadingText(Heading heading) {
        StringBuilder text = new StringBuilder();
        Node child = heading.getFirstChild();
        while (child != null) {
            String childText = extractPlainText(child);
            if (childText != null && !childText.trim().isEmpty()) {
                if (text.length() > 0) {
                    text.append(" ");
                }
                text.append(childText);
            }
            child = child.getNext();
        }
        return text.toString().trim();
    }

    /**
     * 提取节点内容（保留格式，特别是表格）
     */
    private String extractNodeContent(Node node) {
        if (node == null) {
            return null;
        }

        // 对于表格，保留原始 Markdown 格式
        if (node instanceof TableBlock) {
            return extractTableMarkdown(node);
        }

        // 对于其他节点，提取文本内容
        return extractPlainText(node);
    }

    /**
     * 提取表格的原始 Markdown 格式
     */
    private String extractTableMarkdown(Node tableNode) {
        if (originalMarkdownContent == null) {
            return extractPlainText(tableNode);
        }

        try {
            // 获取表格节点在原始文档中的位置
            BasedSequence chars = tableNode.getChars();
            if (chars != null && chars.length() > 0) {
                int startOffset = chars.getStartOffset();
                int endOffset = chars.getEndOffset();

                // 从原始 Markdown 中提取表格内容
                if (startOffset >= 0 && endOffset <= originalMarkdownContent.length() && startOffset < endOffset) {
                    String tableMarkdown = originalMarkdownContent.substring(startOffset, endOffset);
                    return tableMarkdown.trim();
                }
            }

            // 如果无法从原始内容提取，尝试从节点本身提取
            return extractPlainText(tableNode);
        } catch (Exception e) {
            log.warn("提取表格 Markdown 失败，使用文本提取: {}", e.getMessage());
            return extractPlainText(tableNode);
        }
    }

    /**
     * 提取节点的纯文本内容
     */
    private String extractPlainText(Node node) {
        if (node == null) {
            return null;
        }

        StringBuilder text = new StringBuilder();
        extractTextRecursive(node, text);
        return text.length() > 0 ? text.toString().trim() : null;
    }

    /**
     * 递归提取文本
     */
    private void extractTextRecursive(Node node, StringBuilder text) {
        if (node == null) {
            return;
        }

        // 跳过标题节点（标题已经在 extractSections 中单独处理）
        if (node instanceof Heading) {
            return;
        }

        // 对于有子节点的节点，递归处理子节点
        Node child = node.getFirstChild();
        if (child != null) {
            boolean isFirstChild = true;
            while (child != null) {
                // 在子节点之间添加适当的分隔符
                if (!isFirstChild && text.length() > 0) {
                    // 检查是否需要换行
                    if (child instanceof Block) {
                        if (!text.toString().endsWith("\n")) {
                            text.append("\n");
                        }
                    } else {
                        text.append(" ");
                    }
                }
                extractTextRecursive(child, text);
                child = child.getNext();
                isFirstChild = false;
            }
        } else {
            // 叶子节点，尝试提取文本
            try {
                BasedSequence chars = node.getChars();
                if (chars != null && chars.length() > 0) {
                    String nodeText = chars.toString().trim();
                    if (!nodeText.isEmpty()) {
                        if (text.length() > 0 && !text.toString().endsWith("\n")) {
                            text.append(" ");
                        }
                        text.append(nodeText);
                    }
                }
            } catch (Exception e) {
                // 忽略，继续处理
            }
        }
    }
}
