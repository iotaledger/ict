import Cookies from 'js-cookie';

export const get = async (route, password, params) => {
	try {
		const payload = params ? params : {};
		payload.password = password || Cookies.get('password');

		const body = stringify(payload);

		const response = await fetch(`http://localhost:2187/get${route.charAt(0).toUpperCase() + route.slice(1)}`, {
			headers: {
				'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
			},
			method: 'POST',
			body
		});

		if (response.status === 401) {
			return { error: 'Incorrect password' };
		}

		const data = await response.json();

		return typeof data === 'object' && data.success ? data : [];
	} catch (err) {
		console.log(err);
		return [];
	}
};

const stringify = (payload) => {
	return Object.entries(payload)
		.map(([key, value]) => {
			return encodeURIComponent(key) + '=' + encodeURIComponent(value);
		})
		.join('&');
};

export const set = async (route, payload) => {
	try {
		payload.password = Cookies.get('password');

		const body = stringify(payload);

		const response = await fetch(`http://localhost:2187/${route}`, {
			headers: {
				'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
			},
			method: 'POST',
			body
		});

		const data = await response.json();

		return data;
	} catch (err) {
		console.log(err);
		return [];
	}
};
