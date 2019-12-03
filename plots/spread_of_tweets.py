import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

from matplotlib import dates


# Alessia Macari, Valeria Marini, Elenoire Casalegno, Andrea Damante, Pamela Prati

#Distribution over time Tweets
data_am = pd.read_csv("am_tt.csv")
data_am = data_am.iloc[:,:].values

data_vm = pd.read_csv("vm_tt.csv")
data_vm = data_vm.iloc[:,:].values

data_ec = pd.read_csv("ec_tt.csv")
data_ec = data_ec.iloc[:,:].values

data_ad = pd.read_csv("ad_tt.csv")
data_ad = data_ad.iloc[:,:].values

data_pp = pd.read_csv("pp_tt.csv")
data_pp = data_pp.iloc[:,:].values


data_all = np.append(data_am, data_vm)
data_all = np.append(data_all, data_ec)
data_all = np.append(data_all, data_ad)
data_all = np.append(data_all, data_pp)


'''
plt.hist(data)
plt.xlabel("Time")
plt.ylabel("Frequency") 
plt.title("Distribution over time")
#plt.show()

plt.hist(data_y)
plt.xlabel("Time")
plt.ylabel("Frequency")
plt.title("Distribution over time Yes")
#plt.show()

plt.hist(data_n)
plt.xlabel("Time")
plt.ylabel("Frequency")
plt.title("Distribution over time No")
#plt.show()

'''

from datetime import datetime
ts = int("1284101485")

# if you encounter a "year is out of range" error the timestamp
# may be in milliseconds, try `ts /= 1000` in that case
#print(datetime.utcfromtimestamp(ts).strftime('%Y-%m-%d %H:%M:%S'))



def plot_data(data, data_key):
    
    #convert strings into datetime objects
    time_data = [datetime.utcfromtimestamp(int(int(i)/1000)).strftime('%Y-%m-%d %H:%M:%S') for i in data]

    #print(conv_time)

    df = pd.DataFrame(time_data, columns=[data_key])

    df[data_key] = pd.to_datetime(df[data_key])

    #print(df)

    print(df.groupby(by=[df[data_key].dt.month, df[data_key].dt.day]).count().plot(kind="bar"))
    plt.show()
    
    
plot_data(data_am, "AM")

plot_data(data_vm, "VM")

plot_data(data_ec, "EC")

plot_data(data_ad, "AD")

plot_data(data_ad, "PP")


plot_data(data_all, "All")