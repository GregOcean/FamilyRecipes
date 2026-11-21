"""Conservative cleanup of MediaWiki presentation artifacts; no semantic rewriting."""
import re
DIRECTIVE = re.compile(r'__(?:NOTOC|TOC|FORCETOC|NOEDITSECTION|NEWSECTIONLINK|NONEWSECTIONLINK|NOGALLERY|HIDDENCAT|INDEX|NOINDEX)__', re.I)
SUSPECT = re.compile(r'__[A-Za-z_]{2,40}__|\{\{|\}\}|\ufffd')
def clean(text):
    text = DIRECTIVE.sub('', text)
    text = re.sub(r'(?m)^[ \t]*\|[ \t]+(?=\S)', '', text)
    return re.sub(r'\n[ \t]*\n(?:[ \t]*\n)+', '\n\n', text).strip()

def strip_templates(text):
    """Remove balanced nested templates, preserving unmatched fragments for review."""
    out=[];i=0
    while i<len(text):
        if text[i:i+2]!='{{':out.append(text[i]);i+=1;continue
        start=i;depth=1;i+=2
        while i<len(text) and depth:
            if text[i:i+2]=='{{':depth+=1;i+=2
            elif text[i:i+2]=='}}':depth-=1;i+=2
            else:i+=1
        if depth:out.append(text[start:])
    return ''.join(out)
