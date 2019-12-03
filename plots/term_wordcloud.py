import matplotlib.pyplot as plt
from wordcloud import WordCloud

import json
from pprint import pprint


# Alessia Macari - AlessiaMacari1 – 1385 tweets
# Valeria Marini - ValeriaMariniVM – 1200 tweets
# Elenoire Casalegno - elenoirec – 1094 tweets
# Andrea Damante - AndreaDamante – 262 tweets
# Pamela Prati - PamelaPrati – 119 tweets

pList = ['Alessia Macari', 'Valeria Marini', 'Elenoire Casalegno', 'Andrea Damante', 'Pamela Prati']

for p in pList: 

    with open( p + 'Words.json') as f:
        p_words = json.load(f)

    d = {}
    for a in p_words:
        w = a['word']
        f = a['frequency']
        d[w] = f


    print("Word Cloud for " + p + " words")

    wordcloud = WordCloud()
    wordcloud.generate_from_frequencies(frequencies=d)
    plt.figure()
    plt.imshow(wordcloud, interpolation="bilinear")
    plt.axis("off")
    plt.show()