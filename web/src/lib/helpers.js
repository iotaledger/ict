export const toDate = (timestamp, long) => {
	const date = new Date(timestamp).toLocaleString().replace(',', '');
	return long ? date.substr(0, date.length - 3) : date.substr(-8, 5);
};

export const moduleURI = (name, port) => {
	let url = `${window.location.protocol}//${window.location.hostname}`;
	return port ? `${url}:${port}/` : `${url}:${window.location.port}/modules/${name}/`;
};
