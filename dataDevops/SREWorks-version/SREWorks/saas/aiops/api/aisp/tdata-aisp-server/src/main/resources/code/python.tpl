import requests

url = "{{ url }}"
payload={{ input }}
headers = {
  'x-auth-app': '{{ app }}',        # 用自己的鉴权，替换x-auth-app字段。
  'x-auth-key': '{{ key }}',        # 用自己的鉴权，替换x-auth-app字段。
  'x-auth-user': '{{ user }}',      # 用自己的鉴权，替换x-auth-app字段。
  'x-biz-app': 'aiops,sreworks,prod', 
  'x-auth-passwd': '{{ passwd }}',  # 一天有效，请用自己的鉴权，替换x-auth-passwd字段。
  'Content-Type': 'application/json'
}
response = requests.request("POST", url, headers=headers, verify=False, data=payload)
print(response.text)
