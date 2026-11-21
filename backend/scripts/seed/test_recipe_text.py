import unittest
from recipe_text import clean, strip_templates
class RecipeTextTest(unittest.TestCase):
    def test_known_directives_and_category_separator(self):
        self.assertEqual('汤类\n\n1/2 cup + ¼ tsp',clean('__notoc__\n\n| 汤类\n\n\n1/2 cup + ¼ tsp'))
    def test_unknown_symbols_preserved(self):
        self.assertEqual('__UNKNOWN__ 180°C | 1–2 tbsp ♥',clean('__UNKNOWN__ 180°C | 1–2 tbsp ♥'))
    def test_nested_templates(self):
        self.assertEqual('Soup\nText',strip_templates('Soup{{recipe|x={{nested|value}}}}\nText'))
    def test_unmatched_templates_preserved(self):
        self.assertEqual('Soup {{unfinished',strip_templates('Soup {{unfinished'))
    def test_idempotent(self):
        text=clean('__NOTOC__\n| Soups\n\n\nMilk')
        self.assertEqual(text,clean(text))
if __name__=='__main__':unittest.main()
