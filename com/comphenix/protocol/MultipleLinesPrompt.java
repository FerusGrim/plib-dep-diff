package com.comphenix.protocol;

import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.ConversationCanceller;
import org.bukkit.conversations.ExactMatchConversationCanceller;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.StringPrompt;

class MultipleLinesPrompt extends StringPrompt
{
    private static final String KEY = "multiple_lines_prompt";
    private static final String KEY_LAST = "multiple_lines_prompt.last_line";
    private static final String KEY_LINES = "multiple_lines_prompt.linecount";
    private final MultipleConversationCanceller endMarker;
    private final String initialPrompt;
    
    public String removeAccumulatedInput(final ConversationContext context) {
        final Object result = context.getSessionData((Object)"multiple_lines_prompt");
        if (result instanceof StringBuilder) {
            context.setSessionData((Object)"multiple_lines_prompt", (Object)null);
            context.setSessionData((Object)"multiple_lines_prompt.linecount", (Object)null);
            return ((StringBuilder)result).toString();
        }
        return null;
    }
    
    public MultipleLinesPrompt(final String endMarker, final String initialPrompt) {
        this((ConversationCanceller)new ExactMatchConversationCanceller(endMarker), initialPrompt);
    }
    
    public MultipleLinesPrompt(final ConversationCanceller endMarker, final String initialPrompt) {
        this.endMarker = new MultipleWrapper(endMarker);
        this.initialPrompt = initialPrompt;
    }
    
    public MultipleLinesPrompt(final MultipleConversationCanceller endMarker, final String initialPrompt) {
        this.endMarker = endMarker;
        this.initialPrompt = initialPrompt;
    }
    
    public Prompt acceptInput(final ConversationContext context, final String in) {
        StringBuilder result = (StringBuilder)context.getSessionData((Object)"multiple_lines_prompt");
        Integer count = (Integer)context.getSessionData((Object)"multiple_lines_prompt.linecount");
        if (result == null) {
            context.setSessionData((Object)"multiple_lines_prompt", (Object)(result = new StringBuilder()));
        }
        if (count == null) {
            count = 0;
        }
        context.setSessionData((Object)"multiple_lines_prompt.last_line", (Object)in);
        context.setSessionData((Object)"multiple_lines_prompt.linecount", (Object)(++count));
        result.append(in + "\n");
        if (this.endMarker.cancelBasedOnInput(context, in, result, count)) {
            return Prompt.END_OF_CONVERSATION;
        }
        return (Prompt)this;
    }
    
    public String getPromptText(final ConversationContext context) {
        final Object last = context.getSessionData((Object)"multiple_lines_prompt.last_line");
        if (last instanceof String) {
            return (String)last;
        }
        return this.initialPrompt;
    }
    
    private static class MultipleWrapper implements MultipleConversationCanceller
    {
        private ConversationCanceller canceller;
        
        public MultipleWrapper(final ConversationCanceller canceller) {
            this.canceller = canceller;
        }
        
        @Override
        public boolean cancelBasedOnInput(final ConversationContext context, final String currentLine) {
            return this.canceller.cancelBasedOnInput(context, currentLine);
        }
        
        @Override
        public boolean cancelBasedOnInput(final ConversationContext context, final String currentLine, final StringBuilder lines, final int lineCount) {
            return this.cancelBasedOnInput(context, currentLine);
        }
        
        public void setConversation(final Conversation conversation) {
            this.canceller.setConversation(conversation);
        }
        
        public MultipleWrapper clone() {
            return new MultipleWrapper(this.canceller.clone());
        }
    }
    
    public interface MultipleConversationCanceller extends ConversationCanceller
    {
        boolean cancelBasedOnInput(final ConversationContext p0, final String p1);
        
        boolean cancelBasedOnInput(final ConversationContext p0, final String p1, final StringBuilder p2, final int p3);
    }
}
