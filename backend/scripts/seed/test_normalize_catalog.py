import unittest
from normalize_catalog import clean, normalize

class NormalizeTest(unittest.TestCase):
    def test_wiki_link_text_preserved(self):
        self.assertEqual('Boil 125 g flour', clean('Boil 125 [[Cookbook:Gram|g]] [[Cookbook:flour|flour]]', True))
    def test_quantities_and_notes_survive_structured_import(self):
        record=dict(source='UniTools',source_id='test',source_url='https://example.com',author='Author',license='CC BY-SA 4.0',license_url='https://creativecommons.org/licenses/by-sa/4.0/',title='Soup',country='CN',images=[],original=dict(summary={'en':'Soup'},ingredients=[dict(name={'en':'Salt'},quantity=.5,unit='tsp',note={'en':'Do not add more salt than the quantity listed here; especially for children'})],steps=[dict(text={'en':'Do not boil.'})]))
        result,error=normalize(record)
        self.assertIsNone(error)
        self.assertIn('0.5 tsp',result['recipe']['description'])
        self.assertIn('Do not add more',result['recipe']['description'])
        self.assertEqual('Do not boil.',result['steps'][0]['description'])
        self.assertLessEqual(len(result['ingredients'][0]['amount']),50)
        self.assertNotIn('cover_image',result['recipe'])
        self.assertIn('no_verified_cover',result['provenance']['quality_gaps'])
if __name__=='__main__':unittest.main()
