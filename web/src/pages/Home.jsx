import React, { useState, useEffect } from 'react';

import { get } from '../lib/api';

const Home = () => {
	const [version, setVersion] = useState(null);

	useEffect(async () => {
		const getConfig = async () => {
			const info = await get('info');
			setVersion(info.version);
		};
		getConfig();
	}, []);

	return (
		<section className="home">
			<article>
				<h1>
					IOTA Controlled Agent
					{version && (
						<small>
							version <strong>{version}</strong>
						</small>
					)}
				</h1>
			</article>
		</section>
	);
};

export default Home;
