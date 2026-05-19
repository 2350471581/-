import browser_cookie3
import json

try:
    cookies = list(browser_cookie3.edge(domain_name="pc.woozooo.com"))
    result = {c.name: c.value for c in cookies}
    with open(r"X:\BillTracker\lanzou_cookie.json", "w") as f:
        json.dump(result, f)
    print("Cookies saved:", result)
except Exception as e:
    print(f"Error: {type(e).__name__}: {e}")
    try:
        cookies = list(browser_cookie3.chrome(domain_name="pc.woozooo.com"))
        result = {c.name: c.value for c in cookies}
        with open(r"X:\BillTracker\lanzou_cookie.json", "w") as f:
            json.dump(result, f)
        print("Cookies from chrome:", result)
    except Exception as e2:
        print(f"Chrome also failed: {e2}")
